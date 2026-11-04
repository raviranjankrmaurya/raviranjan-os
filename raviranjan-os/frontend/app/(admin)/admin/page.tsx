'use client';

import React from 'react';
import { 
  FolderKanban, CheckCircle2, FileEdit, Activity, TrendingUp, 
  Github, Code, Terminal, Eye, FileText, Mail, Bot, AlertTriangle, 
  Clock, ArrowUpRight, ShieldCheck, RefreshCw 
} from 'lucide-react';

export default function AdminDashboardPage() {
  const statCards = [
    { title: "Total Projects", value: "14", detail: "8 published, 4 draft, 2 archived", icon: FolderKanban, color: "text-sky-400" },
    { title: "Published Systems", value: "8", detail: "100% telemetry online", icon: CheckCircle2, color: "text-emerald-400" },
    { title: "Active Build Sprint", value: "3", detail: "Avg. 72% milestone progress", icon: Activity, color: "text-amber-400" },
    { title: "GitHub Commits (YTD)", value: "1,248", detail: "Last commit 24m ago (main)", icon: Github, color: "text-slate-200" },
    { title: "LeetCode Solved", value: "550", detail: "Top 4.2% global ranking", icon: Code, color: "text-amber-500" },
    { title: "GFG Coding Score", value: "2,150", detail: "420 verified algorithmic solves", icon: Terminal, color: "text-emerald-500" },
    { title: "Portfolio Pageviews", value: "34.2k", detail: "+18.4% vs previous 30d", icon: Eye, color: "text-purple-400" },
    { title: "Resume Downloads", value: "842", detail: "Verified recruiter downloads", icon: FileText, color: "text-blue-400" },
    { title: "Inbound Inquiries", value: "19", detail: "3 new pending review", icon: Mail, color: "text-rose-400" },
    { title: "AI Assistant Queries", value: "1,420", detail: "RAG accuracy rating: 98.7%", icon: Bot, color: "text-cyan-400" },
  ];

  const recentEdits = [
    { type: "PROJECT", title: "Raviranjan.OS Core Platform", user: "raviranjan", time: "12m ago", action: "Updated ADR #4: SSE over WebSockets" },
    { type: "ARTICLE", title: "Designing Resilient Event-Driven Ingestion in Java 21", user: "raviranjan", time: "1h ago", action: "Published v1.0.0" },
    { type: "LAB", title: "High-Throughput SIMD Vector Benchmarks", user: "raviranjan", time: "3h ago", action: "Appended memory allocation results" },
    { type: "SKILL", title: "Kafka & Event Streaming", user: "raviranjan", time: "5h ago", action: "Increased proficiency to 92%" },
  ];

  const integrationStatuses = [
    { platform: "GitHub Enterprise API", status: "HEALTHY", lastSync: "10 mins ago", ping: "42ms" },
    { platform: "LeetCode Public GraphQL", status: "HEALTHY", lastSync: "1 hour ago", ping: "180ms" },
    { platform: "GeeksforGeeks Scraper", status: "HEALTHY", lastSync: "3 hours ago", ping: "210ms" },
    { platform: "WhatsApp Business Cloud", status: "HEALTHY", lastSync: "Live Webhook", ping: "15ms" },
  ];

  return (
    <div className="space-y-8 max-w-7xl mx-auto">
      {/* Welcome & Live Status Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-[#1f2430]">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white font-mono">System Command Center</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time telemetry, portfolio analytics, and content synchronization state.</p>
        </div>
        <div className="flex items-center space-x-3">
          <button className="px-3 py-1.5 rounded-md bg-[#141822] hover:bg-[#1a202c] border border-[#232938] text-xs font-mono text-slate-300 flex items-center gap-1.5 transition-colors">
            <RefreshCw className="w-3.5 h-3.5 text-sky-400" />
            <span>Trigger Global Sync</span>
          </button>
          <div className="px-3 py-1.5 rounded-md bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-mono flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>All Subsystems Operational</span>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {statCards.map((stat, i) => {
          const Icon = stat.icon;
          return (
            <div key={i} className="p-4 rounded-xl bg-[#11131a] border border-[#1f2430] hover:border-[#2d3445] transition-all flex flex-col justify-between">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono text-slate-400">{stat.title}</span>
                <Icon className={`w-4 h-4 ${stat.color}`} />
              </div>
              <div className="my-3">
                <span className="text-2xl font-bold font-mono text-white tracking-tight">{stat.value}</span>
              </div>
              <span className="text-[11px] text-slate-500 truncate">{stat.detail}</span>
            </div>
          );
        })}
      </div>

      {/* Dual Column Operations Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Activity Feed & Pending Tasks */}
        <div className="lg:col-span-2 space-y-6">
          <div className="p-6 rounded-xl bg-[#11131a] border border-[#1f2430]">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-white font-mono flex items-center gap-2">
                <Clock className="w-4 h-4 text-sky-400" /> Recent Content Mutations
              </h3>
              <span className="text-xs text-slate-500 font-mono">Auto-audited</span>
            </div>
            <div className="space-y-3">
              {recentEdits.map((item, idx) => (
                <div key={idx} className="p-3 rounded-lg bg-[#141822] border border-[#1f2430] flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-3">
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">
                      {item.type}
                    </span>
                    <div>
                      <p className="font-medium text-slate-200">{item.title}</p>
                      <p className="text-[11px] text-slate-400">{item.action}</p>
                    </div>
                  </div>
                  <span className="text-[10px] font-mono text-slate-500">{item.time}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Pending Tasks & Roadmap Sprint */}
          <div className="p-6 rounded-xl bg-[#11131a] border border-[#1f2430]">
            <h3 className="text-sm font-semibold text-white font-mono flex items-center gap-2 mb-4">
              <FolderKanban className="w-4 h-4 text-amber-400" /> Active Project Sprints & Milestones
            </h3>
            <div className="space-y-4 text-xs">
              <div className="space-y-2">
                <div className="flex justify-between font-mono">
                  <span className="text-slate-300">Raviranjan.OS — Frontend & Telemetry Pipeline</span>
                  <span className="text-sky-400 font-bold">85%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-[#1e2433] overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-sky-500 to-blue-500 w-[85%] rounded-full"></div>
                </div>
              </div>
              <div className="space-y-2">
                <div className="flex justify-between font-mono">
                  <span className="text-slate-300">Distributed Query Engine with SIMD (Lab POC)</span>
                  <span className="text-amber-400 font-bold">60%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-[#1e2433] overflow-hidden">
                  <div className="h-full bg-gradient-to-r from-amber-500 to-orange-500 w-[60%] rounded-full"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right 1 Col: Integration Status & Quick Actions */}
        <div className="space-y-6">
          <div className="p-6 rounded-xl bg-[#11131a] border border-[#1f2430]">
            <h3 className="text-sm font-semibold text-white font-mono flex items-center gap-2 mb-4">
              <ShieldCheck className="w-4 h-4 text-emerald-400" /> Integration Telemetry
            </h3>
            <div className="space-y-3">
              {integrationStatuses.map((intg, i) => (
                <div key={i} className="p-3 rounded-lg bg-[#141822] border border-[#1f2430] flex flex-col gap-1.5 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-slate-200">{intg.platform}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      {intg.status}
                    </span>
                  </div>
                  <div className="flex justify-between text-[11px] text-slate-500 font-mono">
                    <span>Sync: {intg.lastSync}</span>
                    <span>Latency: {intg.ping}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
