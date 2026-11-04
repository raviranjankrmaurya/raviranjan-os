'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { 
  LayoutDashboard, User, FolderKanban, Milestone, History, GitPullRequest, 
  Lightbulb, Award, GraduationCap, Trophy, FileCheck, BookOpen, FlaskConical, 
  PenTool, GitFork, Binary, Github, Code, Terminal, Network, BarChart3, 
  Mail, Bell, Settings, ShieldCheck, ChevronRight, Search, Command, CheckCircle2
} from 'lucide-react';

interface NavItem {
  name: string;
  href: string;
  icon: React.ElementType;
  badge?: string;
}

const navSections = [
  {
    title: "OPERATING SYSTEM",
    items: [
      { name: "Dashboard", href: "/admin", icon: LayoutDashboard },
      { name: "Profile & Identity", href: "/admin/profile", icon: User },
      { name: "Homepage Builder", href: "/admin/homepage", icon: LayoutDashboard },
      { name: "Site Settings", href: "/admin/settings", icon: Settings },
    ]
  },
  {
    title: "ENGINEERING & PROJECTS",
    items: [
      { name: "Projects", href: "/admin/projects", icon: FolderKanban, badge: "12" },
      { name: "Milestones", href: "/admin/milestones", icon: Milestone },
      { name: "Changelog", href: "/admin/changelog", icon: History },
      { name: "Technical Decisions", href: "/admin/decisions", icon: GitPullRequest },
      { name: "Engineering Lab", href: "/admin/lab", icon: FlaskConical },
    ]
  },
  {
    title: "KNOWLEDGE & CREDENTIALS",
    items: [
      { name: "Skills Matrix", href: "/admin/skills", icon: Lightbulb },
      { name: "Experience", href: "/admin/experience", icon: Award },
      { name: "Education", href: "/admin/education", icon: GraduationCap },
      { name: "Achievements", href: "/admin/achievements", icon: Trophy },
      { name: "Certifications", href: "/admin/certifications", icon: FileCheck },
      { name: "DSA Knowledge", href: "/admin/dsa", icon: Binary },
    ]
  },
  {
    title: "WRITING & RESEARCH",
    items: [
      { name: "Technical Writing", href: "/admin/writing", icon: PenTool },
      { name: "Research Papers", href: "/admin/research", icon: BookOpen },
      { name: "Contribution Stories", href: "/admin/contributions", icon: GitFork },
    ]
  },
  {
    title: "INTEGRATION TELEMETRY",
    items: [
      { name: "GitHub Integration", href: "/admin/github", icon: Github },
      { name: "LeetCode Ingestion", href: "/admin/leetcode", icon: Code },
      { name: "GeeksforGeeks", href: "/admin/gfg", icon: Terminal },
      { name: "Integration Hub", href: "/admin/integrations", icon: Network, badge: "4 Live" },
    ]
  },
  {
    title: "OPERATIONS & CRM",
    items: [
      { name: "Contact & CRM", href: "/admin/crm", icon: Mail, badge: "3 New" },
      { name: "Traffic Analytics", href: "/admin/analytics", icon: BarChart3 },
      { name: "System Notifications", href: "/admin/notifications", icon: Bell },
      { name: "Audit Logs", href: "/admin/audit-logs", icon: ShieldCheck },
    ]
  }
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [searchOpen, setSearchOpen] = useState(false);

  return (
    <div className="flex h-screen bg-[#090a0f] text-slate-100 overflow-hidden font-sans antialiased">
      {/* Sidebar Navigation */}
      <aside className="w-72 border-r border-[#1f2430] bg-[#0c0e14] flex flex-col justify-between shrink-0">
        <div>
          {/* Logo & Platform Identifier */}
          <div className="h-16 px-6 border-b border-[#1f2430] flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-sky-400 to-blue-600 flex items-center justify-center font-mono font-bold text-white shadow-lg shadow-sky-500/20">
                R
              </div>
              <div>
                <span className="font-semibold tracking-tight text-white flex items-center gap-1.5 text-sm">
                  Raviranjan.OS <span className="text-[10px] uppercase font-mono px-1.5 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">PROD</span>
                </span>
                <span className="text-[11px] text-slate-400 font-mono block">Dev Operating System</span>
              </div>
            </div>
          </div>

          {/* Quick Command Bar Trigger */}
          <div className="px-4 py-3">
            <button 
              onClick={() => setSearchOpen(true)}
              className="w-full h-9 px-3 rounded-md bg-[#141822] border border-[#232938] text-xs text-slate-400 flex items-center justify-between hover:border-[#38bdf8]/40 hover:text-slate-200 transition-colors">
              <span className="flex items-center gap-2">
                <Search className="w-3.5 h-3.5" /> Quick search...
              </span>
              <kbd className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-[#1e2433] text-slate-400 border border-[#2b3345]">⌘K</kbd>
            </button>
          </div>

          {/* Nav Links Scroller */}
          <nav className="px-3 overflow-y-auto max-h-[calc(100vh-170px)] space-y-6 pb-6 text-xs">
            {navSections.map((section, idx) => (
              <div key={idx} className="space-y-1">
                <p className="px-3 text-[10px] font-mono tracking-wider font-semibold text-slate-500 uppercase mb-1.5">
                  {section.title}
                </p>
                {section.items.map((item) => {
                  const Icon = item.icon;
                  const isActive = pathname === item.href;
                  return (
                    <Link
                      key={item.name}
                      href={item.href}
                      className={`flex items-center justify-between px-3 py-2 rounded-md font-medium transition-all ${
                        isActive
                          ? 'bg-sky-500/10 text-sky-400 border border-sky-500/30'
                          : 'text-slate-400 hover:text-slate-100 hover:bg-[#141822]'
                      }`}
                    >
                      <div className="flex items-center space-x-2.5">
                        <Icon className={`w-4 h-4 ${isActive ? 'text-sky-400' : 'text-slate-400'}`} />
                        <span>{item.name}</span>
                      </div>
                      {item.badge && (
                        <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded-full ${
                          isActive 
                            ? 'bg-sky-500/20 text-sky-300' 
                            : 'bg-[#1b202c] text-slate-400'
                        }`}>
                          {item.badge}
                        </span>
                      )}
                    </Link>
                  );
                })}
              </div>
            ))}
          </nav>
        </div>

        {/* Footer Status Pill */}
        <div className="p-4 border-t border-[#1f2430] bg-[#0c0e14] flex items-center justify-between text-xs">
          <div className="flex items-center space-x-2.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span className="text-[11px] font-mono text-slate-300">Sync: Healthy</span>
          </div>
          <span className="text-[10px] font-mono text-slate-500">v1.0.4-LTS</span>
        </div>
      </aside>

      {/* Main Content Viewport */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Operational Header */}
        <header className="h-16 border-b border-[#1f2430] bg-[#0c0e14]/70 backdrop-blur-md px-8 flex items-center justify-between shrink-0">
          <div className="flex items-center space-x-2 text-xs font-mono text-slate-400">
            <span>Raviranjan.OS</span>
            <ChevronRight className="w-3.5 h-3.5 text-slate-600" />
            <span className="text-slate-200 capitalize">{pathname.split('/').pop() || 'Dashboard'}</span>
          </div>

          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 bg-[#141822] border border-[#232938] px-3 py-1.5 rounded-md text-xs font-mono text-slate-300">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span>PostgreSQL & Redis Linked</span>
            </div>
            <Link 
              href="/" 
              target="_blank"
              className="text-xs px-3 py-1.5 rounded-md bg-[#1e2433] hover:bg-[#283042] text-slate-200 border border-[#2b3345] transition-colors flex items-center gap-1.5">
              <span>View Public OS</span>
              <ChevronRight className="w-3 h-3" />
            </Link>
          </div>
        </header>

        {/* Dynamic Body Outlet */}
        <main className="flex-1 overflow-y-auto p-8 bg-[#090a0f]">
          {children}
        </main>
      </div>
    </div>
  );
}
