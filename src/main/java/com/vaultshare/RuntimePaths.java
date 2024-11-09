package com.vaultshare;

import java.nio.file.Path;

public record RuntimePaths(Path dataDir, Path uploadsDir) {
}
