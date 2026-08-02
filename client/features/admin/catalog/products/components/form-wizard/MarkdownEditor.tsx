"use client";

import React, { useState } from "react";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Bold, Italic, Code, List } from "lucide-react";

interface MarkdownEditorProps {
  value: string;
  onChange: (val: string) => void;
  registerProps: any; // react-hook-form register props
}

export function MarkdownEditor({ value, onChange, registerProps }: MarkdownEditorProps) {
  const [descTab, setDescTab] = useState<"write" | "preview">("write");

  const handleMarkdownInsert = (syntax: string) => {
    const textarea = document.getElementById("prod-desc") as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    const selected = text.substring(start, end);
    let replacement = "";

    switch (syntax) {
      case "bold":
        replacement = `**${selected || "chữ đậm"}**`;
        break;
      case "italic":
        replacement = `*${selected || "chữ nghiêng"}*`;
        break;
      case "code":
        replacement = `\`${selected || "mã nguồn"}\``;
        break;
      case "list":
        replacement = `\n- ${selected || "mục 1"}`;
        break;
      default:
        return;
    }

    const newValue = text.substring(0, start) + replacement + text.substring(end);
    onChange(newValue);
    textarea.focus();
    setTimeout(() => {
      textarea.setSelectionRange(start + replacement.length, start + replacement.length);
    }, 0);
  };

  const renderMarkdownPreview = (text: string) => {
    if (!text) return <p className="text-slate-400 italic text-sm">Chưa nhập mô tả...</p>;
    let html = text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      // Headers
      .replace(/^### (.*$)/gim, '<h3 class="text-sm font-bold text-slate-800 mt-2 mb-1">$1</h3>')
      .replace(/^## (.*$)/gim, '<h2 class="text-base font-bold text-slate-800 mt-3 mb-1.5">$1</h2>')
      .replace(/^# (.*$)/gim, '<h1 class="text-lg font-bold text-slate-900 mt-4 mb-2">$1</h1>')
      // Bold
      .replace(/\*\*(.*?)\*\*/g, '<strong class="font-bold text-slate-900">$1</strong>')
      // Italic
      .replace(/\*(.*?)\*/g, '<em class="italic">$1</em>')
      // Lists
      .replace(/^\s*-\s+(.*$)/gim, '<li class="list-disc ml-5 text-slate-700">$1</li>')
      // Code blocks
      .replace(/`(.*?)`/g, '<code class="bg-slate-100 text-rose-600 px-1 py-0.5 rounded font-mono text-xs border">$1</code>')
      // Paragraph breaks
      .replace(/\n/g, "<br />");
    return <div dangerouslySetInnerHTML={{ __html: html }} className="prose prose-sm max-w-none text-slate-750" />;
  };

  return (
    <div className="space-y-2">
      <div className="flex justify-between items-center">
        <Label className="text-slate-750 font-bold text-xs">Mô tả chi tiết sản phẩm</Label>
        <div className="flex border border-slate-200 rounded-lg p-0.5 bg-slate-50 gap-0.5 text-xs font-semibold">
          <button
            type="button"
            onClick={() => setDescTab("write")}
            className={`px-3 py-1 rounded-md transition-all cursor-pointer ${
              descTab === "write" ? "bg-white text-slate-800 shadow-2xs" : "text-slate-400 hover:text-slate-700"
            }`}
          >
            Viết (Markdown)
          </button>
          <button
            type="button"
            onClick={() => setDescTab("preview")}
            className={`px-3 py-1 rounded-md transition-all cursor-pointer ${
              descTab === "preview" ? "bg-white text-slate-800 shadow-2xs" : "text-slate-400 hover:text-slate-700"
            }`}
          >
            Xem trước
          </button>
        </div>
      </div>

      {descTab === "write" ? (
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 p-1.5 border border-slate-200 bg-slate-50/50 rounded-t-lg border-b-0">
            <button
              type="button"
              onClick={() => handleMarkdownInsert("bold")}
              className="p-1.5 hover:bg-slate-200 rounded text-slate-600 cursor-pointer"
              title="Chữ đậm"
            >
              <Bold className="size-3.5" />
            </button>
            <button
              type="button"
              onClick={() => handleMarkdownInsert("italic")}
              className="p-1.5 hover:bg-slate-200 rounded text-slate-600 cursor-pointer"
              title="Chữ nghiêng"
            >
              <Italic className="size-3.5" />
            </button>
            <button
              type="button"
              onClick={() => handleMarkdownInsert("code")}
              className="p-1.5 hover:bg-slate-200 rounded text-slate-600 cursor-pointer"
              title="Code inline"
            >
              <Code className="size-3.5" />
            </button>
            <button
              type="button"
              onClick={() => handleMarkdownInsert("list")}
              className="p-1.5 hover:bg-slate-200 rounded text-slate-600 cursor-pointer"
              title="Danh sách mục"
            >
              <List className="size-3.5" />
            </button>
          </div>
          <Textarea
            id="prod-desc"
            {...registerProps}
            placeholder="Mô tả tóm tắt tính năng sản phẩm sử dụng cú pháp Markdown..."
            rows={6}
            className="border-slate-200 rounded-t-none focus-visible:ring-shop_dark_green/10"
          />
        </div>
      ) : (
        <div className="border border-slate-200 rounded-lg p-4 min-h-[162px] bg-slate-50/30 overflow-y-auto">
          {renderMarkdownPreview(value || "")}
        </div>
      )}
    </div>
  );
}
