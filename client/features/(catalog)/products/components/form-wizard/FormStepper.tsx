import React from "react";

interface FormStepperProps {
  activeStep: number;
  onStepClick: (step: number) => void;
}

export function FormStepper({ activeStep, onStepClick }: FormStepperProps) {
  const steps = [
    { num: 1, label: "Thông tin cơ bản" },
    { num: 2, label: "Thuộc tính (EAV)" },
    { num: 3, label: "Quản lý SKU & Giá" },
    { num: 4, label: "Xuất bản & Review" },
  ];

  return (
    <div className="sticky top-16 z-30 bg-white/90 backdrop-blur-md border border-slate-150 py-3 px-6 rounded-xl shadow-xs flex flex-wrap justify-between items-center gap-2">
      {steps.map((s) => {
        const isActive = activeStep === s.num;
        const isDone = activeStep > s.num;
        return (
          <button
            key={s.num}
            type="button"
            onClick={() => onStepClick(s.num)}
            className="flex items-center gap-2 transition-all group cursor-pointer focus:outline-none"
          >
            <div
              className={`size-6 rounded-full flex items-center justify-center text-xs font-black transition-all ${
                isActive
                  ? "bg-slate-900 text-white ring-4 ring-slate-100 scale-105"
                  : isDone
                  ? "bg-emerald-600 text-white"
                  : "bg-slate-50 border-slate-200 text-slate-400"
              }`}
            >
              {isDone ? "✓" : s.num}
            </div>
            <span
              className={`text-xs font-bold transition-colors ${
                isActive ? "text-slate-900 font-extrabold" : isDone ? "text-emerald-700" : "text-slate-455"
              }`}
            >
              Bước {s.num}: {s.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}
