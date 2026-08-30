import type { CatalogProductDetail } from "../../types/catalog.types";

function displayValue(value: string | number | boolean | string[], unit?: string | null) {
  const text = Array.isArray(value)
    ? value.join(", ")
    : typeof value === "boolean"
      ? value
        ? "Có"
        : "Không"
      : String(value);
  return unit ? `${text} ${unit}` : text;
}

export function ProductSpecifications({ product }: { product: CatalogProductDetail }) {
  const definitions = [...product.specificationDefinitions].sort(
    (left, right) => left.sortOrder - right.sortOrder,
  );
  const definitionCodes = new Set(definitions.map((definition) => definition.code));
  const rows = [
    ...definitions.flatMap((definition) => {
      const value = product.attributes[definition.code];
      return value === undefined || value === null || value === ""
        ? []
        : [{ code: definition.code, name: definition.name, value: displayValue(value, definition.unit) }];
    }),
    ...Object.entries(product.attributes)
      .filter(([code, value]) => !definitionCodes.has(code) && value !== null && value !== "")
      .sort(([left], [right]) => left.localeCompare(right, "vi"))
      .map(([code, value]) => ({ code, name: code, value: displayValue(value) })),
  ];

  if (!rows.length) return null;

  return (
    <section className="mt-8 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
      <h2 className="text-xl font-extrabold text-slate-950">Thông số sản phẩm</h2>
      <dl className="mt-5 overflow-hidden rounded-2xl border border-slate-200">
        {rows.map((row, index) => (
          <div
            key={row.code}
            className={`grid grid-cols-1 gap-1 px-4 py-3 text-sm sm:grid-cols-[minmax(180px,0.4fr)_1fr] sm:gap-5 ${
              index % 2 === 0 ? "bg-slate-50" : "bg-white"
            }`}
          >
            <dt className="font-bold text-slate-700">{row.name}</dt>
            <dd className="text-slate-600">{row.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
