"use client";

import { useState, ReactNode } from "react";
import { Loader2, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { ChipInput } from "@/components/outreach/chip-input";
import { generateOutreach } from "@/lib/api";
import type { GenerateOutreachRequest, OutreachResponseDto } from "@/lib/types";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onGenerated: (resp: OutreachResponseDto) => void;
}

interface FormState {
  fullName: string;
  role: string;
  companyName: string;
  companyDomain: string;
  linkedinUrl: string;
  githubUrl: string;
  signals: string[];
  contextNotes: string;
}

const INITIAL: FormState = {
  fullName: "",
  role: "",
  companyName: "",
  companyDomain: "",
  linkedinUrl: "",
  githubUrl: "",
  signals: [],
  contextNotes: "",
};

export function GenerateSheet({ open, onOpenChange, onGenerated }: Props) {
  const [form, setForm] = useState<FormState>(INITIAL);
  const [error, setError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const update = <K extends keyof FormState>(k: K, v: FormState[K]) => {
    setForm((prev) => ({ ...prev, [k]: v }));
  };

  const hasIdentity =
    form.linkedinUrl.trim().length > 0 ||
    form.githubUrl.trim().length > 0 ||
    (form.fullName.trim().length > 0 && form.role.trim().length > 0);
  const canSubmit = hasIdentity && form.companyName.trim().length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit || isGenerating) return;
    setError(null);
    setIsGenerating(true);
    try {
      const payload: GenerateOutreachRequest = {
        fullName: form.fullName.trim() || null,
        role: form.role.trim() || null,
        companyName: form.companyName.trim(),
        companyDomain: form.companyDomain.trim() || null,
        linkedinUrl: form.linkedinUrl.trim() || null,
        githubUrl: form.githubUrl.trim() || null,
        techStackSignals: form.signals,
        contextNotes: form.contextNotes.trim() || null,
      };
      const resp = await generateOutreach(payload);
      onGenerated(resp);
      setForm(INITIAL);
      onOpenChange(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Generation failed";
      setError(message);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-[480px]">
        {isGenerating && (
          <div className="absolute left-0 right-0 top-0 h-0.5 overflow-hidden">
            <div className="h-full w-full bg-accent animate-pulse" />
          </div>
        )}
        <SheetHeader>
          <SheetTitle>Generate outreach</SheetTitle>
          <SheetDescription>
            Provide what you know about the prospect. The AI fills in the rest.
          </SheetDescription>
        </SheetHeader>

        <form
          onSubmit={handleSubmit}
          className="flex flex-1 flex-col gap-4 overflow-y-auto px-5 py-5"
        >
          <FormRow label="Full name">
            <Input
              placeholder="e.g. Sven Müller"
              value={form.fullName}
              onChange={(v) => update("fullName", v)}
            />
          </FormRow>
          <FormRow label="Role">
            <Input
              placeholder="e.g. Head of AI"
              value={form.role}
              onChange={(v) => update("role", v)}
            />
          </FormRow>
          <FormRow label="Company" required>
            <Input
              placeholder="e.g. Helia Robotics"
              value={form.companyName}
              onChange={(v) => update("companyName", v)}
            />
          </FormRow>
          <FormRow label="Company domain">
            <Input
              placeholder="e.g. helia.ai"
              value={form.companyDomain}
              onChange={(v) => update("companyDomain", v)}
            />
          </FormRow>
          <FormRow label="LinkedIn URL">
            <Input
              placeholder="https://linkedin.com/in/…"
              value={form.linkedinUrl}
              onChange={(v) => update("linkedinUrl", v)}
            />
          </FormRow>
          <FormRow label="GitHub URL">
            <Input
              placeholder="https://github.com/…"
              value={form.githubUrl}
              onChange={(v) => update("githubUrl", v)}
            />
          </FormRow>
          <FormRow label="Tech stack signals">
            <ChipInput
              value={form.signals}
              onChange={(next) => update("signals", next)}
              placeholder="Add signal…"
            />
          </FormRow>
          <FormRow label="Context notes">
            <textarea
              rows={3}
              placeholder="What's happening at their company? Recent news? Pain points?"
              value={form.contextNotes}
              onChange={(e) => update("contextNotes", e.target.value)}
              className="w-full rounded-sm border border-border-subtle bg-bg-base px-3 py-2 text-sm text-text-primary placeholder:text-text-tertiary focus:border-accent focus:outline-none resize-none transition-colors"
            />
          </FormRow>

          {error && (
            <p className="text-[12px] text-critical">{error}</p>
          )}
        </form>

        <SheetFooter className="flex flex-col gap-2">
          <Button
            type="submit"
            variant="default"
            onClick={handleSubmit}
            disabled={!canSubmit || isGenerating}
            className="w-full"
          >
            {isGenerating ? (
              <>
                <Loader2 className="mr-1.5 h-3 w-3 animate-spin" />
                Generating…
              </>
            ) : (
              <>
                Generate
                <Sparkles className="ml-1.5 h-3 w-3" />
              </>
            )}
          </Button>
          <span className="block text-center text-[11px] text-text-tertiary">
            Typically takes 5-7 seconds
          </span>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

function FormRow({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col">
      <span className="text-[11px] font-medium uppercase tracking-wider text-text-tertiary mb-1.5">
        {label}
        {required && <span className="ml-1 text-accent">*</span>}
      </span>
      {children}
    </div>
  );
}

function Input({
  value,
  onChange,
  placeholder,
}: {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}) {
  return (
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="w-full rounded-sm border border-border-subtle bg-bg-base px-3 py-2 text-sm text-text-primary placeholder:text-text-tertiary focus:border-accent focus:outline-none transition-colors"
    />
  );
}
