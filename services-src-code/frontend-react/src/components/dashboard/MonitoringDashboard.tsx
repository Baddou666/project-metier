import { useEffect, useState } from "react";
import { motion } from "motion/react";
import { 
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, 
  AreaChart, Area, PieChart, Pie, Cell 
} from "recharts";
import { 
  Activity, 
  Clock, 
  TrendingUp, 
  FileText
} from "lucide-react";
import { subscribeToRecentDetections, DetectionResult } from "../../services/localDetections";
import { format } from "date-fns";
import { cn } from "../../lib/utils";

interface MonitoringDashboardProps {
  onSelectDetection?: (detection: DetectionResult) => void;
}

function getTextExcerpt(detection: DetectionResult) {
  const text = (detection.text ?? detection.textPreview).replace(/\s+/g, " ").trim();
  return text.length > 140 ? `${text.slice(0, 140)}...` : text;
}

export default function MonitoringDashboard({ onSelectDetection }: MonitoringDashboardProps) {
  const [detections, setDetections] = useState<DetectionResult[]>([]);
  const [stats, setStats] = useState({
    total: 0,
    aiCount: 0,
    humanCount: 0,
    avgLatency: 0,
    avgConfidence: 0
  });

  useEffect(() => {
    const unsubscribe = subscribeToRecentDetections((data) => {
      setDetections(data);
      
      const ai = data.filter(d => d.label === 'AI').length;
      const human = data.filter(d => d.label === 'HUMAN').length;
      const total = data.length;
      const avgLat = total > 0 ? data.reduce((acc, curr) => acc + curr.latency, 0) / total : 0;
      const avgConf = total > 0 ? data.reduce((acc, curr) => acc + curr.confidence, 0) / total : 0;

      setStats({
        total,
        aiCount: ai,
        humanCount: human,
        avgLatency: Math.round(avgLat),
        avgConfidence: Math.round(avgConf * 100)
      });
    });

    return () => unsubscribe();
  }, []);

  const chartData = detections.slice().reverse().map(d => ({
    time: format(d.timestamp, 'HH:mm:ss'),
    latency: d.latency,
    inputLength: d.inputLength ?? d.textPreview.length
  }));

  const labelData = [
    { name: 'AI Generated', value: stats.aiCount, color: '#fb923c' },
    { name: 'Human Authored', value: stats.humanCount, color: '#60a5fa' }
  ];

  return (
    <div className="pt-24 pb-12 px-4 max-w-7xl mx-auto space-y-8">
      {/* Metrics Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: "Analyses", value: stats.total, icon: FileText, detail: "Textes verifies dans cette session" },
          { label: "Temps moyen", value: `${stats.avgLatency}ms`, icon: Clock, detail: "Duree moyenne par analyse" },
          { label: "Score IA moyen", value: `${stats.avgConfidence}%`, icon: TrendingUp, detail: "Pres de 0 humain, pres de 1 IA" },
          { label: "Textes IA", value: stats.total > 0 ? `${Math.round((stats.aiCount / stats.total) * 100)}%` : "0%", icon: Activity, detail: "Part des textes classes comme IA" }
        ].map((m, i) => (
          <motion.div
            key={m.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            className="glass p-6 shadow-xl shadow-black/20"
          >
            <div className="flex items-center justify-between mb-4">
              <m.icon className="w-5 h-5 text-indigo-400 opacity-60" />
              <span className="text-[10px] font-mono font-bold text-green-400 bg-green-400/10 px-2 py-0.5 rounded-full uppercase tracking-tighter shadow-[0_0_8px_rgba(74,222,128,0.2)]">Session</span>
            </div>
            <p className="text-3xl font-sans font-black text-white tracking-tighter">{m.value}</p>
            <p className="text-[10px] font-mono text-white/40 uppercase mt-1 tracking-widest">{m.label}</p>
            <div className="mt-4 pt-4 border-t border-white/5">
              <p className="text-[9px] text-white/30 font-medium">{m.detail}</p>
            </div>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Chart */}
        <div className="lg:col-span-2 glass p-6 text-white shadow-xl shadow-black/20">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h3 className="font-sans font-bold text-xs uppercase tracking-[0.2em] text-white/40">Temps d'analyse et longueur du texte</h3>
              <p className="text-[10px] font-mono opacity-20 mt-1">Analyses recentes de cette session</p>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-white/20" />
                <span className="text-[10px] font-mono opacity-30 uppercase tracking-tighter">Temps</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.5)]" />
                <span className="text-[10px] font-mono opacity-30 uppercase tracking-tighter">Longueur</span>
              </div>
            </div>
          </div>

          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData}>
                <defs>
                  <linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#818cf8" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#818cf8" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="inputLengthGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2}/>
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#ffffff05" vertical={false} />
                <XAxis 
                  dataKey="time" 
                  stroke="#ffffff20" 
                  fontSize={9} 
                  tickLine={false} 
                  axisLine={false}
                  dy={10}
                />
                <YAxis 
                  stroke="#ffffff20" 
                  fontSize={9} 
                  tickLine={false} 
                  axisLine={false} 
                  tickFormatter={(val) => `${val}`}
                />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#141414', border: '1px solid #ffffff10', borderRadius: '12px', fontSize: '10px', backdropFilter: 'blur(10px)' }}
                  itemStyle={{ fontSize: '10px', color: '#fff' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="latency" 
                  stroke="#818cf8" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#latencyGradient)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="inputLength" 
                  name="Longueur du texte"
                  stroke="#3b82f6" 
                  strokeWidth={3}
                  fillOpacity={1}
                  fill="url(#inputLengthGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Distribution Chart */}
        <div className="glass p-6 shadow-xl shadow-black/20 relative">
          <h3 className="font-sans font-bold text-xs uppercase tracking-[0.2em] text-white/40 mb-8">Repartition des resultats</h3>
          <div className="h-[250px] w-full flex items-center justify-center">
             <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={labelData}
                  cx="50%"
                  cy="50%"
                  innerRadius={70}
                  outerRadius={90}
                  paddingAngle={8}
                  dataKey="value"
                  stroke="none"
                >
                  {labelData.map((entry, index) => (
                    <Cell 
                      key={`cell-${index}`} 
                      fill={entry.color} 
                      className="drop-shadow-lg"
                      style={{ filter: `drop-shadow(0 0 8px ${entry.color}40)` }}
                    />
                  ))}
                </Pie>
                <Tooltip 
                   contentStyle={{ backgroundColor: '#141414', border: 'none', borderRadius: '8px', fontSize: '10px' }}
                />
              </PieChart>
             </ResponsiveContainer>
             <div className="absolute top-[50%] left-[50%] translate-x-[-50%] translate-y-[-10%] flex flex-col items-center justify-center pointer-events-none">
                <span className="text-4xl font-black text-white tracking-tighter">{stats.total}</span>
                <span className="text-[8px] font-mono text-white/30 uppercase tracking-[0.2em]">Analyses</span>
             </div>
          </div>
          <div className="grid grid-cols-2 gap-3 mt-6">
            {labelData.map(l => (
              <div key={l.name} className="flex flex-col items-center p-3 rounded-xl bg-white/5 border border-white/5 transition-all hover:bg-white/10">
                <div className="w-2 h-2 rounded-full mb-2" style={{ backgroundColor: l.color, boxShadow: `0 0 10px ${l.color}` }} />
                <span className="text-xl font-bold text-white tracking-tighter">{l.value}</span>
                <span className="text-[8px] font-mono opacity-30 uppercase text-center leading-tight tracking-widest">{l.name.split(' ')[0]}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Logs Table */}
      <div className="glass overflow-hidden shadow-xl shadow-black/20">
        <div className="bg-white/5 px-6 py-4 border-b border-white/10 flex items-center justify-between">
          <h3 className="font-sans font-bold text-[11px] uppercase tracking-[0.2em] text-white/60">Historique des analyses</h3>
          <div className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
            <span className="font-mono text-[9px] text-white/30 uppercase tracking-widest">Session active</span>
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-white/5">
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Date</th>
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Texte</th>
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Resultat</th>
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Score IA</th>
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Temps</th>
                <th className="px-6 py-5 font-mono text-[9px] uppercase tracking-widest text-white/30">Memo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {detections.map((d) => (
                <tr
                  key={d.id}
                  onClick={() => onSelectDetection?.(d)}
                  className="hover:bg-white/5 group transition-colors cursor-pointer"
                >
                  <td className="px-6 py-5 font-mono text-[10px] text-white/40 whitespace-nowrap">{format(d.timestamp, 'MMM dd, HH:mm:ss')}</td>
                  <td className="px-6 py-5 max-w-xl font-medium text-[11px] text-white/80 italic">
                    <div className="line-clamp-2 leading-relaxed">
                      "{getTextExcerpt(d)}"
                    </div>
                  </td>
                  <td className="px-6 py-5">
                    <span className={cn(
                      "px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest inline-block border",
                      d.label === 'AI' 
                        ? "bg-orange-500/10 text-orange-400 border-orange-500/20" 
                        : "bg-blue-500/10 text-blue-400 border-blue-500/20"
                    )}>
                      {d.label === 'AI' ? 'Genere par IA' : 'Humain'}
                    </span>
                  </td>
                  <td className="px-6 py-5 font-mono text-[11px] font-bold text-white/60">{(d.confidence * 100).toFixed(0)}%</td>
                  <td className="px-6 py-5 font-mono text-[10px] text-white/40">{d.latency}ms</td>
                  <td className="px-6 py-5">
                    <div className="flex items-center gap-2">
                      <div className="w-1 h-4 bg-green-500/30 rounded-full overflow-hidden">
                        <div className="w-full h-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.5)]" />
                      </div>
                      <span className="text-[9px] font-mono text-white/20 uppercase tracking-widest">Session</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {detections.length === 0 && (
            <div className="py-24 text-center opacity-20 italic text-sm font-mono tracking-widest">
              Aucune analyse dans cette session.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

