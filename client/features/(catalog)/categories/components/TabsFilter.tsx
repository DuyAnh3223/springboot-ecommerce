import React from 'react';

interface TabsFilterProps {
  currentIsActive: string | null;
  onChange: (value: string) => void;
}

const TabsFilter = ({ currentIsActive, onChange }: TabsFilterProps) => {
  const tabs = [
    { label: "Tất cả", value: "" },
    { label: "Hoạt động", value: "true" },
    { label: "Bản nháp", value: "false" },
  ];

  const activeValue = currentIsActive || "";

  return (
    <div className="flex items-center gap-1 mb-1.5">
      {tabs.map((tab) => {
        const isActive = activeValue === tab.value;
        return (
          <button
            key={tab.value}
            onClick={() => onChange(tab.value)}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer ${
              isActive
                ? "bg-white border border-slate-200 text-slate-900 shadow-sm"
                : "text-slate-500 hover:text-slate-800 hover:bg-slate-100/50"
            }`}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
};

export default TabsFilter;