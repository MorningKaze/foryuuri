package mgKze;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class PingMCServer {
    public static byte PACKET_HANDSHAKE = 0x00,
            PACKET_STATUSREQUEST = 0x00,
            PACKET_PING = 0x01;
    public static int PROTOCOL_VERSION = 4;
    public static int STATUS_HANDSHAKE = 1;
    public static String CHARSET = "UTF-8";

    public static Reply getReply(String ip) throws IOException {
        String[] ips = ip.split(":");
        String host = ips[0];
        int port = Integer.parseInt(ips[1]);

        final Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 2000);

        final DataInputStream in = new DataInputStream(socket.getInputStream());
        final DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        //> Handshake

        ByteArrayOutputStream handshake_bytes = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(handshake_bytes);

        handshake.writeByte(PACKET_HANDSHAKE);
        writeVarInt(handshake, PROTOCOL_VERSION);
        writeVarInt(handshake, host.length());
        handshake.writeBytes(host);
        handshake.writeShort(port);
        writeVarInt(handshake, STATUS_HANDSHAKE);

        writeVarInt(out, handshake_bytes.size());
        out.write(handshake_bytes.toByteArray());

        //> Status request

        out.writeByte(0x01); // Size of packet
        out.writeByte(PACKET_STATUSREQUEST);

        //< Status response

        readVarInt(in); // Size
        int id = readVarInt(in);

        io(id == -1, "这只服务器姬暂时不想理你~ (Server prematurely ended stream.)");
        io(id != PACKET_STATUSREQUEST, "这只服务器姬没有返回有效数据包~ (Server returned invalid packet.)");

        int length = readVarInt(in);
        io(length == -1, "这只服务器姬暂时不想理你~ (Server prematurely ended stream.)");
        io(length == 0, "这只服务器姬的返回值太神必~ (Server returned unexpected value.)");

        byte[] data = new byte[length];
        in.readFully(data);
        String json = new String(data, CHARSET);

        //> Ping

        out.writeByte(0x09); // Size of packet
        out.writeByte(PACKET_PING);
        out.writeLong(System.currentTimeMillis());

        //< Ping

        readVarInt(in); // Size
        id = readVarInt(in);
        io(id == -1, "这只服务器姬暂时不想理你~ (Server prematurely ended stream.)");
        //io(id != PACKET_PING, "这只服务器姬没有返回有效数据包~ (Server returned invalid packet.)");

        // Close

        handshake.close();
        handshake_bytes.close();
        out.close();
        in.close();
        socket.close();

        return new Gson().fromJson(json, Reply.class);
    }

    public static void io(final boolean b, final String m) throws IOException {
        if (b) {
            throw new IOException(m);
        }
    }

    /**
     * @author thinkofdeath
     * See: https://gist.github.com/thinkofdeath/e975ddee04e9c87faf22
     */
    public static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();

            i |= (k & 0x7F) << j++ * 7;

            if (j > 5)
                throw new RuntimeException("VarInt too big");

            if ((k & 0x80) != 128)
                break;
        }

        return i;
    }

    /**
     * @author thinkofdeath
     * See: https://gist.github.com/thinkofdeath/e975ddee04e9c87faf22
     * @throws IOException
     */
    public static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
}

/**
 * References:
 * http://wiki.vg/Server_List_Ping
 * https://gist.github.com/thinkofdeath/6927216
 */
class Reply {

    private Players players;
    private Version version;
    private String favicon;

    /**
     * @return @{link Players}
     */
    public Players getPlayers() {
        return this.players;
    }

    /**
     * @return @{link Version}
     */
    public Version getVersion() {
        return this.version;
    }

    /**
     * @return Base64 encoded favicon image
     */
    public String getFavicon() {
        return this.favicon;
    }

    public class Players {
        private int max;
        private int online;
        private List<Player> sample;

        /**
         * @return Maximum player count
         */
        public int getMax() {
            return this.max;
        }

        /**
         * @return Online player count
         */
        public int getOnline() {
            return this.online;
        }

        /**
         * @return List of some players (if any) specified by server
         */
        public List<Player> getSample() {
            return this.sample;
        }
    }

    public class Player {
        private String name;
        private String id;

        /**
         * @return Name of player
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return Unknown
         */
        public String getId() {
            return this.id;
        }

    }

    public class Version {
        private String name;
        private int protocol;

        /**
         * @return Version name (ex: 13w41a)
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return Protocol version
         */
        public int getProtocol() {
            return this.protocol;
        }
    }

}
