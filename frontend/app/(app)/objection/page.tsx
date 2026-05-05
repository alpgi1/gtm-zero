import { Badge } from "@/components/ui/badge";

export default function ObjectionPage() {
  return (
    <div className="flex flex-col items-start gap-3">
      <span className="text-xs uppercase tracking-wider text-text-tertiary">
        Part 7 of 11
      </span>
      <h1 className="text-2xl font-semibold text-text-primary tracking-tight">
        Objection Handling
      </h1>
      <p className="text-sm text-text-secondary max-w-xl">
        Live retrieval-augmented Q&amp;A surface where prospects&apos; technical
        objections get answered with citations to your source documents.
      </p>
      <Badge variant="default" className="mt-2">
        Under construction
      </Badge>
    </div>
  );
}
