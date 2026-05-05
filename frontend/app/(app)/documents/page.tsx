import { Badge } from "@/components/ui/badge";

export default function DocumentsPage() {
  return (
    <div className="flex flex-col items-start gap-3">
      <span className="text-xs uppercase tracking-wider text-text-tertiary">
        Part 8 of 11
      </span>
      <h1 className="text-2xl font-semibold text-text-primary tracking-tight">
        Documents
      </h1>
      <p className="text-sm text-text-secondary max-w-xl">
        Manage the source corpus the RAG pipeline retrieves from. Upload,
        re-ingest, and inspect chunk-level coverage per document.
      </p>
      <Badge variant="default" className="mt-2">
        Under construction
      </Badge>
    </div>
  );
}
