package com.edgedemo.nativebridge

object NativeBridge {
    init {
        try {
            System.loadLibrary("edge_native")
        } catch (_: Throwable) { }
    }

    external fun processEdgesRgba(
        rgbaIn: ByteArray,
        width: Int,
        height: Int,
        useCanny: Boolean
    ): ByteArray
}



