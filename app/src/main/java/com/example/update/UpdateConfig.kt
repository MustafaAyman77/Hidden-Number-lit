package com.example.update

object UpdateConfig {

    /**
     * GitHub repository containing the game's releases.
     *
     * Format:
     * owner/repository
     */
    const val GITHUB_REPOSITORY =
        "MustafaAyman77/Hidden-Number-lit"

    /**
     * APK extension expected in GitHub Releases.
     */
    const val APK_EXTENSION =
        ".apk"

    /**
     * Maximum number of HTTP redirects.
     */
    const val MAX_REDIRECTS =
        10

    /**
     * Network connection timeout.
     */
    const val CONNECT_TIMEOUT =
        15_000

    /**
     * Network read timeout.
     */
    const val READ_TIMEOUT =
        30_000
}
