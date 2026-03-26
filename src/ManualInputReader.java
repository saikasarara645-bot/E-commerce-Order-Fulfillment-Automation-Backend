public class ManualInputReader extends java.io.Reader {
    private java.io.InputStream in;

    public ManualInputReader(java.io.InputStream in) {
        this.in = in;
    }

    public int read(char[] cbuf, int off, int len) throws java.io.IOException {
        if (cbuf == null) {
            throw new NullPointerException("Buffer is null");
        }
        if (off < 0 || len < 0 || off + len > cbuf.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }
        if (len == 0) {
            return 0;
        }

        int count = 0;

        while (count < len) {
            int b = in.read();

            if (b == -1) {
                return (count == 0) ? -1 : count;
            }

            cbuf[off + count] = (char) (b & 0xFF);
            count++;

            if (b == '\n') {
                break;
            }
        }

        return count;
    }
    public void close() throws java.io.IOException {
        // Do not close System.in here.
    }
}