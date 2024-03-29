package site.yan.core.data;

public class Host {

    private String serverName;
    private String address;
    private int port;

    public Host() {
    }

    public Host(Host hostCopy) {
        this.serverName = hostCopy.serverName;
        this.address = hostCopy.address;
        this.port = hostCopy.port;
    }

    public Host(String serverName, String address, int port) {
        this.serverName = serverName;
        this.address = address;
        this.port = port;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
