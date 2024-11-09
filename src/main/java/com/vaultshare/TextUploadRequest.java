package com.vaultshare;

public record TextUploadRequest(long duration, String text, String pass, boolean burn) {
}
