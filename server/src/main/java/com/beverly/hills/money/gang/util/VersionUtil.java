package com.beverly.hills.money.gang.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public interface VersionUtil {


    static int getMajorVersion(final String version) {
        int defaultValue = -1;
        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version can't be blank");
        }
        int indexOfFirstDot = version.indexOf(".");
        if (indexOfFirstDot == defaultValue) {
            throw new IllegalArgumentException("Invalid version format");
        }
        int majorVersion = NumberUtils.toInt(version.substring(0, indexOfFirstDot), defaultValue);
        if (majorVersion == defaultValue) {
            throw new IllegalArgumentException("Can't get major version");
        }
        return majorVersion;
    }
}
