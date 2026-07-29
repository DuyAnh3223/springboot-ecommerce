package spring.abtechzone.modules.product.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Attribute;

public final class AttributeUtils {

    private AttributeUtils() {
        // Utility class
    }

    public static Set<Object> extractAllowedEnumValues(Attribute def) {
        List<Object> options = def.getEnumValues();
        if (options == null || options.isEmpty()) {
            throw new AppException(ErrorCode.ATTRIBUTE_ENUM_VALUES_MISSING);
        }

        Set<Object> allowed = new HashSet<>();
        for (Object opt : options) {
            if (opt instanceof Map<?, ?> map) {
                Object v = map.get("value");
                if (v != null) {
                    allowed.add(v);
                }
            } else {
                allowed.add(opt);
            }
        }
        return allowed;
    }
}
