"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, Sparkles, ArrowRight } from "lucide-react";
import { HERO_SLIDES, SUB_BANNERS } from "../home.config";

export default function HeroSection() {
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    if (isPaused) return;
    const interval = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % HERO_SLIDES.length);
    }, 5000);
    return () => clearInterval(interval);
  }, [isPaused]);

  const activeSlide = HERO_SLIDES[currentSlide];

  return (
    <section className="py-6 bg-slate-900 text-white overflow-hidden border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
          {/* Main Hero Slider (70% on Desktop) */}
          <div
            className="lg:col-span-8 relative rounded-2xl overflow-hidden bg-slate-950 border border-slate-800 shadow-2xl min-h-[380px] sm:min-h-[420px] flex flex-col justify-between p-6 sm:p-10 group"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
          >
            {/* Background Image Overlay */}
            <div className="absolute inset-0 z-0">
              <img
                src={activeSlide.image}
                alt={activeSlide.title}
                className="w-full h-full object-cover object-center opacity-40 group-hover:scale-105 transition-transform duration-700"
              />
              <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-950/80 to-transparent" />
            </div>

            {/* Slide Content */}
            <div className="relative z-10 max-w-xl space-y-4 my-auto">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-600/90 text-rose-100 text-xs font-bold uppercase tracking-wider backdrop-blur-xs">
                <Sparkles className="w-3.5 h-3.5" />
                {activeSlide.badge}
              </span>

              <h1 className="text-2xl sm:text-4xl font-extrabold text-white leading-tight tracking-tight drop-shadow-md">
                {activeSlide.title}
              </h1>

              <p className="text-slate-300 text-sm sm:text-base font-medium line-clamp-2">
                {activeSlide.subtitle}
              </p>

              {activeSlide.highlightText && (
                <div className="inline-block bg-slate-900/90 border border-amber-500/40 text-amber-300 text-xs px-3 py-1.5 rounded-lg font-semibold">
                  🎁 {activeSlide.highlightText}
                </div>
              )}

              <div className="pt-2">
                <Link
                  href={activeSlide.ctaLink}
                  className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow-lg shadow-rose-900/30 hover:shadow-rose-600/50 transition-all group/btn"
                >
                  <span>{activeSlide.ctaText}</span>
                  <ArrowRight className="w-4 h-4 group-hover/btn:translate-x-1 transition-transform" />
                </Link>
              </div>
            </div>

            {/* Slider Controls */}
            <div className="relative z-10 flex items-center justify-between pt-4 mt-auto">
              <div className="flex items-center gap-2">
                {HERO_SLIDES.map((_, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setCurrentSlide(idx)}
                    aria-label={`Chuyển tới slide ${idx + 1}`}
                    className={`h-2 rounded-full transition-all duration-300 ${
                      idx === currentSlide
                        ? "w-8 bg-rose-500"
                        : "w-2 bg-slate-700 hover:bg-slate-500"
                    }`}
                  />
                ))}
              </div>

              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() =>
                    setCurrentSlide(
                      (prev) => (prev - 1 + HERO_SLIDES.length) % HERO_SLIDES.length
                    )
                  }
                  aria-label="Slide trước"
                  className="w-8 h-8 rounded-lg bg-slate-900/80 hover:bg-rose-600 border border-slate-700 text-white flex items-center justify-center transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() =>
                    setCurrentSlide((prev) => (prev + 1) % HERO_SLIDES.length)
                  }
                  aria-label="Slide sau"
                  className="w-8 h-8 rounded-lg bg-slate-900/80 hover:bg-rose-600 border border-slate-700 text-white flex items-center justify-center transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* Sub Banners Stack (30% on Desktop) */}
          <div className="lg:col-span-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-1 gap-4">
            {SUB_BANNERS.map((sub) => (
              <div
                key={sub.id}
                className={`relative rounded-2xl overflow-hidden border border-slate-800 bg-gradient-to-br ${sub.bgGradient} p-5 flex flex-col justify-between min-h-[195px] group hover:border-slate-700 transition-all`}
              >
                <div className="absolute inset-0 z-0">
                  <img
                    src={sub.image}
                    alt={sub.title}
                    className="w-full h-full object-cover opacity-25 group-hover:scale-105 transition-transform duration-500"
                  />
                </div>

                <div className="relative z-10 space-y-2">
                  <span className="inline-block px-2.5 py-0.5 rounded-md bg-amber-500/20 text-amber-300 border border-amber-500/30 text-[11px] font-bold">
                    {sub.badge}
                  </span>
                  <h3 className="text-base font-bold text-white leading-snug line-clamp-1">
                    {sub.title}
                  </h3>
                  <p className="text-slate-300 text-xs line-clamp-2">
                    {sub.subtitle}
                  </p>
                </div>

                <div className="relative z-10 pt-3">
                  <Link
                    href={sub.ctaLink}
                    className="inline-flex items-center gap-1 text-xs font-bold text-rose-400 hover:text-white transition-colors"
                  >
                    Xem sản phẩm <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
