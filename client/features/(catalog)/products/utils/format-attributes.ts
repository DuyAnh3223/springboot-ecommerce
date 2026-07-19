import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";

export function formatAttributesForSubmit(
  categoryAttributes: CategoryAttributeResponse[],
  nonVariantValues: Record<string, any>
): Record<string, any> {
  const formattedAttributes: Record<string, any> = {};
  categoryAttributes
    .filter((ca) => !ca.isVariantDefining)
    .forEach((ca) => {
      const val = nonVariantValues[ca.code];
      if (ca.dataType === "NUMBER") {
        if (ca.isMultiValue) {
          let list: any[] = [];
          if (typeof val === "string") {
            list = val.split(",").map((s) => s.trim()).filter(Boolean);
          } else if (Array.isArray(val)) {
            list = val;
          }
          const nums = list
            .map((item) => parseFloat(item))
            .filter((num) => !isNaN(num));
          if (nums.length > 0) {
            formattedAttributes[ca.code] = nums;
          }
        } else {
          if (val !== undefined && val !== null && val !== "") {
            const num = parseFloat(val);
            if (!isNaN(num)) {
              formattedAttributes[ca.code] = num;
            }
          }
        }
      } else if (ca.dataType === "BOOLEAN") {
        if (val === "true" || val === true) {
          formattedAttributes[ca.code] = true;
        } else if (val === "false" || val === false) {
          formattedAttributes[ca.code] = false;
        }
      } else {
        if (ca.isMultiValue) {
          let list: any[] = [];
          if (typeof val === "string") {
            list = val.split(",").map((s) => s.trim()).filter(Boolean);
          } else if (Array.isArray(val)) {
            list = val;
          }
          if (list.length > 0) {
            formattedAttributes[ca.code] = list;
          }
        } else {
          if (val !== undefined && val !== null && val !== "") {
            const strVal = typeof val === "string" ? val.trim() : val;
            if (strVal !== "") {
              formattedAttributes[ca.code] = strVal;
            }
          }
        }
      }
    });

  return formattedAttributes;
}
