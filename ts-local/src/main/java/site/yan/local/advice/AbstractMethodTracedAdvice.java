package site.yan.local.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import site.yan.core.cache.TraceCache;
import site.yan.core.data.Note;
import site.yan.core.data.Record;
import site.yan.core.enumeration.NoteType;
import site.yan.core.helper.RecordContextHolder;
import site.yan.core.utils.ReflectUtil;
import site.yan.core.utils.TimeUtil;
import site.yan.local.constant.LocalPairType;

import java.lang.reflect.Method;
import java.util.Objects;

public abstract class AbstractMethodTracedAdvice {

    public AbstractMethodTracedAdvice() {
        // Nothing to do.
    }

    protected abstract void pointcut();

    @Around("pointcut()")
    public Object process(ProceedingJoinPoint pjp) throws Throwable {

        if (!RecordContextHolder.getCurrentOpenState()) {
            return pjp.proceed();
        } else {
            Record record = Record.createClientRecord();
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();

            String parentId = RecordContextHolder.getServiceId();
            record.setName("method." + method.getName())
                    .setParentId(parentId);
            record.putAdditionalPair(LocalPairType.METHOD_NAME.text(), method.getName());
            record.putAdditionalPair(LocalPairType.RETURN_TYPE.text(), method.getReturnType().getName());

            Note startNote = new Note(NoteType.LOCAL_START.text(), record.getStartTimeStamp(), RecordContextHolder.getHost());
            record.addNotePair(startNote);

            Object[] args = pjp.getArgs();
            String argValue;
            StringBuilder argValueBuilder;
            if (args.length == 0) {
                argValue = "null";
            } else {
                argValue = argsToString(args);
            }
            record.putAdditionalPair(LocalPairType.ARGS.text(), argValue);

            // 执行具体的方法
            Object result = null;
            try {
                result = pjp.proceed();
            } catch (Exception exc) {
                record.setError(true);
                record.putAdditionalPair(LocalPairType.EXCEPTION.text(), exc.getMessage());
                throw exc;
            } finally {
                long currentTime = TimeUtil.stamp();
                record.setDurationTime(currentTime - record.getStartTimeStamp());
                record.putAdditionalPair(LocalPairType.RETURN_VALUE.text(), Objects.isNull(result) ? null : result.toString());
                record.addNotePair(new Note(NoteType.LOCAL_END.text(), currentTime, RecordContextHolder.getHost()));
                TraceCache.put(record);
            }
            return result;
        }
    }

    private String argsToString(Object[] args) {
        StringBuilder argValueBuilder = new StringBuilder("(");

        for (int i = 0; i < args.length; ++i) {
            argValueBuilder.append(ReflectUtil.argToString(args[i]));
            if (i < args.length - 1) {
                argValueBuilder.append(",");
            }
        }
        argValueBuilder.append(")");
        return argValueBuilder.toString();
    }
}

