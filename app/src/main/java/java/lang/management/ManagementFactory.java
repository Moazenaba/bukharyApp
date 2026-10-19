package java.lang.management;

public class ManagementFactory {
    private static final MemoryMXBean dummyMemory = new MemoryMXBean() {
        @Override
        public MemoryUsage getHeapMemoryUsage() {
            return new MemoryUsage(0, 64 * 1024 * 1024, 256 * 1024 * 1024, 512 * 1024 * 1024);
        }
    };

    public static MemoryMXBean getMemoryMXBean() {
        return dummyMemory;
    }
}
