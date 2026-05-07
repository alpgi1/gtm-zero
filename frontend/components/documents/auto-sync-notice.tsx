"use client";

import { GitBranch, Github } from "lucide-react";
import { Button } from "@/components/ui/button";

export function AutoSyncNotice() {
  return (
    <div className="mt-8 rounded-md border border-border-subtle bg-bg-surface p-4">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 flex h-6 w-6 items-center justify-center rounded-sm bg-bg-elevated">
          <GitBranch className="h-3.5 w-3.5 text-text-tertiary" strokeWidth={1.75} />
        </div>
        <div className="flex flex-col gap-1">
          <span className="text-[13px] font-medium text-text-primary">
            Auto-sync from your Git repository
          </span>
          <span className="max-w-xl text-[12px] text-text-secondary">
            Connect a GitHub repository to automatically re-embed your README,
            technical docs, and API specs on every push. Your sales engineer
            always speaks the truth your engineering team is shipping.
          </span>
          <div className="mt-1.5">
            <Button variant="quiet" size="sm" disabled>
              <Github className="mr-1.5 h-3 w-3" />
              Connect repository
              <span className="ml-2 font-mono text-[10px] text-text-tertiary">
                soon
              </span>
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
