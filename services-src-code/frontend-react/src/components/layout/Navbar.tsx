import { Search, Activity, Shield, HardDrive, User } from "lucide-react";
import { cn } from "../../lib/utils";

interface NavbarProps {
  activeTab: 'detector' | 'dashboard';
  setActiveTab: (tab: 'detector' | 'dashboard') => void;
}

export default function Navbar({ activeTab, setActiveTab }: NavbarProps) {
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-white/10 bg-white/5 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-indigo-600 rounded-lg flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <Shield className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="font-sans font-bold tracking-tight text-white text-lg">AI Text Detector</span>
            <span className="ml-2 font-mono text-[10px] opacity-40 uppercase tracking-widest bg-white/10 px-1.5 py-0.5 rounded">v1.0.0</span>
          </div>
        </div>

        <div className="flex items-center gap-1 bg-white/5 p-1 rounded-lg border border-white/5">
          <button
            onClick={() => setActiveTab('detector')}
            className={cn(
              "flex items-center gap-2 px-4 py-1.5 rounded-md text-sm font-medium transition-all duration-200",
              activeTab === 'detector' ? "bg-white/10 text-white shadow-sm ring-1 ring-white/20" : "text-white/50 hover:text-white hover:bg-white/5"
            )}
          >
            <Search className="w-4 h-4" />
            Analyse
          </button>
          <button
            onClick={() => setActiveTab('dashboard')}
            className={cn(
              "flex items-center gap-2 px-4 py-1.5 rounded-md text-sm font-medium transition-all duration-200",
              activeTab === 'dashboard' ? "bg-white/10 text-white shadow-sm ring-1 ring-white/20" : "text-white/50 hover:text-white hover:bg-white/5"
            )}
          >
            <Activity className="w-4 h-4" />
            Historique
          </button>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 px-4 py-2 bg-white/5 border border-white/10 rounded-lg">
            <HardDrive className="w-4 h-4 text-indigo-400" />
            <span className="text-[10px] text-white/50 font-mono uppercase tracking-widest">Session locale</span>
          </div>
          <button
            type="button"
            disabled
            title="Login coming soon"
            className="flex items-center gap-2 px-5 py-2 bg-white/5 text-white/30 border border-white/10 rounded-lg text-sm font-bold cursor-not-allowed"
          >
            <User className="w-4 h-4" />
            Login
            <span className="text-[9px] font-mono uppercase tracking-widest text-white/25">Bientot</span>
          </button>
        </div>
      </div>
    </nav>
  );
}
