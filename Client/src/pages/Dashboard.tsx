import { Link, useNavigate } from "react-router-dom";
import { Plus, Zap, FolderOpen, Activity, AlertTriangle, DollarSign, Clock } from "lucide-react";
import { AppLayout } from "@/components/layout";
import { PageHeader, StatsCard, DataTable, StatusBadge } from "@/components/shared";
import { Button } from "@/components/ui/button";

// Mock data
const stats = {
  functions: 12,
  files: 47,
  invocations: 1243,
  errors: 3,
  estimatedCost: 4.32,
};

const recentActivity = [
  {
    id: 1,
    type: "invocation",
    name: "process-payment",
    status: "success" as const,
    timestamp: "2 min ago",
    duration: "124ms",
  },
  {
    id: 2,
    type: "deployment",
    name: "user-auth",
    status: "success" as const,
    timestamp: "15 min ago",
    duration: "2.3s",
  },
  {
    id: 3,
    type: "invocation",
    name: "send-notification",
    status: "error" as const,
    timestamp: "23 min ago",
    duration: "89ms",
  },
  {
    id: 4,
    type: "invocation",
    name: "resize-image",
    status: "success" as const,
    timestamp: "1 hour ago",
    duration: "456ms",
  },
  {
    id: 5,
    type: "upload",
    name: "config.json",
    status: "success" as const,
    timestamp: "2 hours ago",
    duration: "1.2s",
  },
];

const activityColumns = [
  {
    key: "type",
    header: "Type",
    render: (item: typeof recentActivity[0]) => (
      <span className="text-muted-foreground capitalize">{item.type}</span>
    ),
  },
  {
    key: "name",
    header: "Resource",
    render: (item: typeof recentActivity[0]) => (
      <span className="font-mono text-sm">{item.name}</span>
    ),
  },
  {
    key: "status",
    header: "Status",
    render: (item: typeof recentActivity[0]) => (
      <StatusBadge status={item.status}>
        {item.status === "success" ? "Success" : "Failed"}
      </StatusBadge>
    ),
  },
  {
    key: "duration",
    header: "Duration",
    className: "text-right",
    render: (item: typeof recentActivity[0]) => (
      <span className="text-muted-foreground">{item.duration}</span>
    ),
  },
  {
    key: "timestamp",
    header: "Time",
    className: "text-right",
    render: (item: typeof recentActivity[0]) => (
      <span className="text-muted-foreground">{item.timestamp}</span>
    ),
  },
];

export default function Dashboard() {
  const navigate = useNavigate();

  const handleActivityClick = (item: typeof recentActivity[0]) => {
    if (item.type === 'upload') {
      navigate('/files');
    } else if (item.type === 'invocation') {
      navigate('/invocations');
    } else {
      navigate(`/functions/${item.name}`); // Assuming name matches ID or navigates to list
    }
  };

  return (
    <AppLayout>
      <PageHeader
        title="Dashboard"
        description="Overview of your serverless infrastructure"
        actions={
          <Button asChild>
            <Link to="/functions/create">
              <Plus className="h-4 w-4 mr-2" />
              Create Function
            </Link>
          </Button>
        }
      />

      {/* Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 mb-8">
        <Link to="/functions" className="block transition-transform hover:scale-[1.02]">
          <StatsCard
            title="Functions"
            value={stats.functions}
            icon={Zap}
            className="h-full cursor-pointer hover:border-primary/50"
          />
        </Link>
        <Link to="/files" className="block transition-transform hover:scale-[1.02]">
          <StatsCard
            title="Files"
            value={stats.files}
            icon={FolderOpen}
            className="h-full cursor-pointer hover:border-primary/50"
          />
        </Link>
        <Link to="/invocations" className="block transition-transform hover:scale-[1.02]">
          <StatsCard
            title="Invocations (24h)"
            value={stats.invocations.toLocaleString()}
            icon={Activity}
            trend={{ value: 12, positive: true }}
            className="h-full cursor-pointer hover:border-primary/50"
          />
        </Link>
        <Link to="/logs" className="block transition-transform hover:scale-[1.02]">
          <StatsCard
            title="Errors"
            value={stats.errors}
            icon={AlertTriangle}
            className="h-full cursor-pointer hover:border-primary/50"
          />
        </Link>
        <Link to="/billing" className="block transition-transform hover:scale-[1.02]">
          <StatsCard
            title="Est. Cost"
            value={`$${stats.estimatedCost.toFixed(2)}`}
            subtitle="This month"
            icon={DollarSign}
            className="h-full cursor-pointer hover:border-primary/50"
          />
        </Link>
      </div>

      {/* Recent Activity */}
      <div className="bg-surface border border-border rounded-md">
        <div className="flex items-center justify-between px-4 py-3 border-b border-border">
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-muted-foreground" />
            <h2 className="text-sm font-medium text-foreground">Recent Activity</h2>
          </div>
          <Link
            to="/invocations"
            className="text-xs text-primary hover:underline"
          >
            View all
          </Link>
        </div>
        <DataTable
          columns={activityColumns}
          data={recentActivity}
          onRowClick={handleActivityClick}
        />
      </div>
    </AppLayout>
  );
}
