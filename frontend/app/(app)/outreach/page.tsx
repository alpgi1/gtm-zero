import { Badge } from "@/components/ui/badge";

export default function OutreachPage() {
  return (
    <div className="flex flex-col items-start gap-3">
      <span className="text-xs uppercase tracking-wider text-text-tertiary">
        Part 8 of 11
      </span>
      <h1 className="text-2xl font-semibold text-text-primary tracking-tight">
        Outreach
      </h1>
      <p className="text-sm text-text-secondary max-w-xl">
        Generate hyper-personalized cold outreach messages from a prospect
        URL — every line traceable to the input signal it leaned on.
      </p>
      <Badge variant="default" className="mt-2">
        Under construction
      </Badge>
    </div>
  );
}
