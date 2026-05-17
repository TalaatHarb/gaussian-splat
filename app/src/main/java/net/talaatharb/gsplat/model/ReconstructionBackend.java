package net.talaatharb.gsplat.model;

public enum ReconstructionBackend {
    COLMAP("COLMAP (SfM)"),
    VGGT("VGGT");

    private final String displayName;

    ReconstructionBackend(String displayName) {
        this.displayName = displayName;
    }

    public static ReconstructionBackend fromValue(String value) {
        if (value == null || value.isBlank()) {
            return COLMAP;
        }
        for (ReconstructionBackend backend : values()) {
            if (backend.name().equalsIgnoreCase(value)) {
                return backend;
            }
        }
        return COLMAP;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
