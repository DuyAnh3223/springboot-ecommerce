import { Truck, ShieldCheck, CreditCard, Headphones } from "lucide-react";
import { TRUST_BADGES } from "../home.config";

const iconMap: Record<string, React.ReactNode> = {
  truck: <Truck className="w-6 h-6 text-rose-500" />,
  shield: <ShieldCheck className="w-6 h-6 text-emerald-500" />,
  creditCard: <CreditCard className="w-6 h-6 text-indigo-500" />,
  headphones: <Headphones className="w-6 h-6 text-amber-500" />,
};

export default function TrustBadges() {
  return (
    <section className="py-6 bg-white border-b border-slate-200/80 text-slate-900 shadow-xs">
      <div className="max-w-7xl mx-auto px-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {TRUST_BADGES.map((item) => (
            <div
              key={item.id}
              className="flex items-start gap-3.5 p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 hover:border-rose-300 hover:bg-white hover:shadow-md transition-all group"
            >
              <div className="p-2.5 rounded-xl bg-white border border-slate-200 shadow-xs group-hover:scale-110 transition-transform">
                {iconMap[item.iconName]}
              </div>
              <div className="space-y-1">
                <h4 className="text-xs sm:text-sm font-bold text-slate-800 group-hover:text-rose-600 transition-colors">
                  {item.title}
                </h4>
                <p className="text-[11px] sm:text-xs text-slate-500 leading-snug">
                  {item.description}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
