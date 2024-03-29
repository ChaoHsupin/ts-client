package site.yan.mysql.interceptor;


import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.conf.HostInfo;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.yan.core.data.Host;
import site.yan.core.data.Note;
import site.yan.core.data.Record;
import site.yan.core.delayed.RecordStash;
import site.yan.core.enumeration.NoteType;
import site.yan.core.helper.RecordContextHolder;
import site.yan.core.utils.IdGeneratorHelper;
import site.yan.core.utils.TimeUtil;
import site.yan.mysql.constant.MysqlPairType;

import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class TSQueryInterceptor implements QueryInterceptor {

    private static final String[] filterRegex = {"set autocommit", "set session", "commit",};
    private static final String DEFAULT_SERVICE_NAME = "mysql.";
    private static final String SELECT = "select";
    private static final String UPDATE = "update";
    private static final String INSERT = "insert";
    private static final String DELETE = "delete";

    static ThreadLocal<Record> mysqlRecord = new ThreadLocal<Record>() {
        @Override
        protected Record initialValue() {
            return new Record(false);
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(TSQueryInterceptor.class);

    @Override
    public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
        return new TSQueryInterceptor();
    }

    @Override
    public <T extends Resultset> T preProcess(Supplier<String> sql, Query interceptedQuery) {
        if (!intercept(sql)) {
            return null;
        }
        mysqlRecord.get()
                .setId(IdGeneratorHelper.idLen32Generat())
                .setTraceId(RecordContextHolder.getTraceId())
                .setParentId(RecordContextHolder.getServiceId())
                .setServerName(RecordContextHolder.getServerName())
                .setStage(RecordContextHolder.getStage())
                .setStartTimeStamp(TimeUtil.stamp())
                .setName(DEFAULT_SERVICE_NAME + getTypeName(sql));
        return null;
    }

    @Override
    public <T extends Resultset> T postProcess(Supplier<String> sql, Query interceptedQuery, T originalResultSet, ServerSession serverSession) {
        if (!intercept(sql)) {
            return null;
        }
        Record record = mysqlRecord.get();
        long currentTime = TimeUtil.stamp();
        String mysqlServerName = interceptedQuery.getSession().getHostInfo().getHostProperties().get("serverName");
        record.setDurationTime(currentTime - record.getStartTimeStamp());
        HostInfo hostInfo = interceptedQuery.getSession().getHostInfo();
        Host mysqlHost = new Host(mysqlServerName, hostInfo.getHost(), hostInfo.getPort());
        Note startNote = new Note(NoteType.CLIENT_SEND.text(), record.getStartTimeStamp(), RecordContextHolder.getHost());
        Note endNote = new Note(NoteType.CLIENT_RECEIVE.text(), currentTime, mysqlHost);
        record.addNotePair(startNote, endNote);
        record.putAdditionalPair(MysqlPairType.SQL.text(), sql.get());
        record.putAdditionalPair(MysqlPairType.RESULT_TYPE.text(), interceptedQuery.getResultType().name());
        int resultSize = (Objects.isNull(originalResultSet) || Objects.isNull(originalResultSet.getRows())) ? 0 : originalResultSet.getRows().size();
        record.putAdditionalPair(MysqlPairType.RESULT_SIZE.text(), String.valueOf(resultSize));
        record.putAdditionalPair(MysqlPairType.MYSQL_SERVER_NAME.text(), mysqlServerName);

        Record recordCopy = new Record(record);
        RecordStash.putRecord(recordCopy);
        record.clear();
        return null;
    }

    private boolean intercept(Supplier<String> sql) {
        if (!RecordContextHolder.getCurrentOpenState()) {
            return false;
        }

        if (sql.get() == null) {
            return false;
        }
        for (String regex : filterRegex) {
            String lowCase = sql.get().toLowerCase();
            if (Pattern.compile(regex).matcher(lowCase).find()) {
                return false;
            }
        }
        return true;
    }

    private String getTypeName(Supplier<String> sql) {
        String sqlText = sql.get();
        if (sqlText == null) {
            return "unknown";
        }
        String queryType = sqlText.trim().substring(0, 6).toLowerCase();
        if (SELECT.equals(queryType)) {
            return SELECT;
        } else if (UPDATE.equals(queryType)) {
            return UPDATE;
        } else if (DELETE.equals(queryType)) {
            return DELETE;
        } else if (INSERT.equals(queryType)) {
            return INSERT;
        } else {
            return "unknown";
        }
    }

    @Override
    public boolean executeTopLevelOnly() {
        return true;
    }

    @Override
    public void destroy() {

    }
}
