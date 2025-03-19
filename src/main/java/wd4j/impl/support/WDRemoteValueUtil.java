package wd4j.impl.support;

import wd4j.api.options.BoundingBox;
import wd4j.impl.webdriver.type.script.WDEvaluateResult;
import wd4j.impl.webdriver.type.script.WDPrimitiveProtocolValue;
import wd4j.impl.webdriver.type.script.WDRemoteValue;

import javax.annotation.Nullable;
import java.util.Map;

public class WDRemoteValueUtil {

    @Nullable
    public static String getStringFromEvaluateResult(WDEvaluateResult result) {
        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remoteValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remoteValue instanceof WDPrimitiveProtocolValue.StringValue) {
                return ((WDPrimitiveProtocolValue.StringValue) remoteValue).getValue();
            }
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Error while querying DOM property: " +
                    ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails());
        }
        return null;
    }

    @Nullable
    public static Boolean getBooleanFromEvaluateResult(WDEvaluateResult result) {
        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remoteValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remoteValue instanceof WDPrimitiveProtocolValue.BooleanValue) {
                return ((WDPrimitiveProtocolValue.BooleanValue) remoteValue).getValue();
            }
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Error while querying DOM property: " +
                    ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails());
        }
        return null;
    }

    @Nullable
    public static BoundingBox getBoundingBoxFromEvaluateResult(WDEvaluateResult result) {
        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remoteValue = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();
            if (remoteValue instanceof WDRemoteValue.ObjectRemoteValue) {
                Map<WDRemoteValue, WDRemoteValue> values = ((WDRemoteValue.ObjectRemoteValue) remoteValue).getValue();

                return new BoundingBox(
                        extractDouble(values, "x"),
                        extractDouble(values, "y"),
                        extractDouble(values, "width"),
                        extractDouble(values, "height")
                );
            }
        } else if (result instanceof WDEvaluateResult.WDEvaluateResultError) {
            throw new RuntimeException("Error evaluating boundingBox: " +
                    ((WDEvaluateResult.WDEvaluateResultError) result).getExceptionDetails());
        }
        return null;
    }
    public static double extractDouble(Map<WDRemoteValue, WDRemoteValue> values, String key) {
        for (Map.Entry<WDRemoteValue, WDRemoteValue> entry : values.entrySet()) {
            if (entry.getKey() instanceof WDPrimitiveProtocolValue.StringValue &&
                    key.equals(((WDPrimitiveProtocolValue.StringValue) entry.getKey()).getValue())) {
                WDRemoteValue value = entry.getValue();
                if (value instanceof WDPrimitiveProtocolValue.NumberValue) {
                    return Double.parseDouble(((WDPrimitiveProtocolValue.NumberValue) value).getValue());
                }
            }
        }
        throw new IllegalStateException("Missing key: " + key);
    }
}
