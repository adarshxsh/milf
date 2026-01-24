import { useState } from "react";
import { Search, Copy, ChevronDown, ChevronRight } from "lucide-react";
import { AppLayout } from "@/components/layout";
import { PageHeader } from "@/components/shared";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { cn } from "@/lib/utils";

// Mock log data
const logs = [
  {
    id: "1",
    requestId: "req-abc123",
    timestamp: "2024-01-20 14:32:15.234",
    function: "process-payment",
    level: "info",
    message: "Function invoked",
    details: "Incoming request from 192.168.1.100",
  },
  {
    id: "2",
    requestId: "req-abc123",
    timestamp: "2024-01-20 14:32:15.456",
    function: "process-payment",
    level: "info",
    message: "Processing payment: $99.00 USD",
    details: "Method: card, Provider: stripe",
  },
  {
    id: "3",
    requestId: "req-abc123",
    timestamp: "2024-01-20 14:32:16.123",
    function: "process-payment",
    level: "info",
    message: "Payment successful: txn_abc123",
    details: "Duration: 889ms, Memory: 45MB",
  },
  {
    id: "4",
    requestId: "req-def456",
    timestamp: "2024-01-20 14:31:45.001",
    function: "send-notification",
    level: "error",
    message: "Failed to send notification",
    details: "Error: Connection timeout after 30s\nStack trace:\n  at sendEmail (handler.go:45)\n  at main (main.go:12)",
  },
  {
    id: "5",
    requestId: "req-ghi789",
    timestamp: "2024-01-20 14:30:22.567",
    function: "resize-image",
    level: "warn",
    message: "Image size exceeds recommended limit",
    details: "Size: 15.2MB, Recommended: 10MB",
  },
  {
    id: "6",
    requestId: "req-jkl012",
    timestamp: "2024-01-20 14:28:11.890",
    function: "user-auth",
    level: "info",
    message: "User authenticated successfully",
    details: "User ID: usr_12345, Method: JWT",
  },
];

export default function Logs() {
  const [searchQuery, setSearchQuery] = useState("");
  const [levelFilter, setLevelFilter] = useState("all");
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const toggleRow = (id: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const filteredLogs = logs.filter((log) => {
    const matchesSearch =
      log.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.function.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.requestId.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesLevel = levelFilter === "all" || log.level === levelFilter;
    return matchesSearch && matchesLevel;
  });

  const getLevelColor = (level: string) => {
    switch (level) {
      case "error":
        return "text-status-error";
      case "warn":
        return "text-status-warning";
      case "info":
        return "text-muted-foreground";
      default:
        return "text-muted-foreground";
    }
  };

  return (
    <AppLayout>
      <PageHeader
        title="Logs"
        description="View execution logs across all functions"
      />

      {/* Filters */}
      <div className="flex items-center gap-4 mb-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            type="search"
            placeholder="Search logs..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 h-8 bg-background"
          />
        </div>
        <Select value={levelFilter} onValueChange={setLevelFilter}>
          <SelectTrigger className="w-32 h-8 bg-background">
            <SelectValue placeholder="Level" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Levels</SelectItem>
            <SelectItem value="info">Info</SelectItem>
            <SelectItem value="warn">Warning</SelectItem>
            <SelectItem value="error">Error</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Log List */}
      <div className="bg-terminal border border-border rounded-md overflow-hidden">
        <div className="divide-y divide-border">
          {filteredLogs.map((log) => {
            const isExpanded = expandedRows.has(log.id);
            return (
              <div key={log.id} className="group">
                <button
                  onClick={() => toggleRow(log.id)}
                  className="w-full flex items-start gap-3 px-4 py-2 text-left hover:bg-surface-hover micro-transition"
                >
                  <span className="mt-0.5 text-muted-foreground">
                    {isExpanded ? (
                      <ChevronDown className="h-3.5 w-3.5" />
                    ) : (
                      <ChevronRight className="h-3.5 w-3.5" />
                    )}
                  </span>
                  <span className="text-xs font-mono text-muted-foreground shrink-0 w-44">
                    {log.timestamp}
                  </span>
                  <span
                    className={cn(
                      "text-xs font-mono uppercase shrink-0 w-12",
                      getLevelColor(log.level)
                    )}
                  >
                    {log.level}
                  </span>
                  <span className="text-xs font-mono text-primary shrink-0 w-32 truncate">
                    {log.function}
                  </span>
                  <span className="text-sm text-foreground flex-1 truncate">
                    {log.message}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-6 w-6 opacity-0 group-hover:opacity-100"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigator.clipboard.writeText(log.message);
                    }}
                  >
                    <Copy className="h-3 w-3" />
                  </Button>
                </button>
                
                {isExpanded && (
                  <div className="px-4 pb-3 pl-12 animate-fade-in">
                    <div className="bg-background/50 rounded-md p-3 text-xs font-mono">
                      <div className="flex items-center gap-4 mb-2 text-muted-foreground">
                        <span>Request ID: {log.requestId}</span>
                      </div>
                      <pre className="whitespace-pre-wrap text-foreground">{log.details}</pre>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </AppLayout>
  );
}
