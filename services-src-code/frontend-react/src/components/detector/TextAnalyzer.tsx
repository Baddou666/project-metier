import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Terminal, Send, Loader2, Info, AlertTriangle, CheckCircle2, Cpu, User, Activity } from "lucide-react";
import { classifyText, Classification } from "../../services/aiDetectorApi";
import { DetectionResult, findDetectionByText, logDetection } from "../../services/localDetections";
import { cn } from "../../lib/utils";

interface TextAnalyzerProps {
  selectedDetection?: DetectionResult | null;
}

export default function TextAnalyzer({ selectedDetection }: TextAnalyzerProps) {
  const [text, setText] = useState("");
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [result, setResult] = useState<Classification & { latency: number } | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedDetection) {
      return;
    }

    setText(selectedDetection.text ?? selectedDetection.textPreview);
    setResult({
      label: selectedDetection.label,
      confidence: selectedDetection.confidence,
      latency: selectedDetection.latency,
      reasoning: "Loaded from session history.",
    });
    setError(null);
  }, [selectedDetection]);

  const handleAnalyze = async () => {
    if (!text.trim() || text.length < 50) {
      setError("Text must be at least 50 characters for accurate analysis.");
      return;
    }

    setIsAnalyzing(true);
    setError(null);
    const startTime = Date.now();

    try {
      const cachedDetection = await findDetectionByText(text);
      if (cachedDetection) {
        setResult({
          label: cachedDetection.label,
          confidence: cachedDetection.confidence,
          latency: cachedDetection.latency,
          reasoning: "Resultat recupere depuis la session.",
        });
        return;
      }

      const analysis = await classifyText(text);
      const latency = Date.now() - startTime;
      const finalResult = { ...analysis, latency };
      
      setResult(finalResult);
      
      await logDetection({
        text,
        inputLength: text.length,
        textPreview: text.substring(0, 100),
        label: analysis.label,
        confidence: analysis.confidence,
        latency: latency
      });
    } catch (err: any) {
      setError(err.message || "An error occurred during analysis.");
    } finally {
      setIsAnalyzing(false);
    }
  };

  return (
    <div className="pt-24 pb-12 px-4 max-w-5xl mx-auto">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Input Section */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass overflow-hidden flex flex-col h-[500px] shadow-2xl shadow-indigo-500/5">
            <div className="bg-white/5 px-4 py-3 flex items-center justify-between border-b border-white/10">
              <div className="flex items-center gap-2">
                <Terminal className="w-4 h-4 text-indigo-400" />
                <span className="font-mono text-[10px] text-white/60 uppercase tracking-widest">Texte a analyser</span>
              </div>
              <span className="font-mono text-[10px] text-white/30 uppercase">{text.length} chars</span>
            </div>
            
            <textarea
              value={text}
              onChange={(e) => {
                setText(e.target.value);
                if (error) setError(null);
              }}
              placeholder="Collez ici un texte d'au moins 50 caracteres pour estimer s'il semble humain ou genere par IA..."
              className="flex-1 bg-transparent p-6 font-mono text-sm resize-none focus:outline-none text-white placeholder:text-white/20"
            />

            <div className="p-4 border-t border-white/10 flex items-center justify-between bg-white/5">
              <div className="flex items-center gap-4 text-[10px] font-mono text-white/40 uppercase">
                <span className="flex items-center gap-1"><Info className="w-3 h-3" /> Minimum 50 chars</span>
              </div>
              <button
                onClick={handleAnalyze}
                disabled={isAnalyzing || text.length < 50}
                className={cn(
                  "flex items-center gap-2 px-8 py-2.5 rounded-lg font-bold text-xs uppercase tracking-widest transition-all",
                  isAnalyzing || text.length < 50 
                    ? "bg-white/5 text-white/20 cursor-not-allowed border border-white/5"
                    : "bg-indigo-600 text-white hover:bg-indigo-500 active:scale-95 shadow-lg shadow-indigo-500/20"
                )}
              >
                {isAnalyzing ? <Loader2 className="w-3 h-3 animate-spin" /> : <Send className="w-3 h-3" />}
                {isAnalyzing ? "Processing..." : "Lancer l'analyse"}
              </button>
            </div>
          </div>

          <AnimatePresence>
            {error && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="bg-red-500/10 border border-red-500/20 p-4 rounded-xl flex items-start gap-3"
              >
                <AlertTriangle className="w-5 h-5 text-red-500 shrink-0" />
                <p className="text-sm text-red-200 font-medium">{error}</p>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Results Section */}
        <div className="space-y-6">
          <div className="glass p-6 text-white min-h-[400px] flex flex-col">
            <div className="flex items-center justify-between mb-8">
              <h3 className="font-sans font-bold text-xs uppercase tracking-[0.2em] opacity-40">Resultat estime</h3>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse shadow-[0_0_8px_rgba(34,197,94,0.5)]" />
                <span className="text-[10px] font-mono opacity-40 uppercase">Pret</span>
              </div>
            </div>

            {result ? (
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex-1 flex flex-col space-y-8"
              >
                <div className="text-center py-8 border-y border-white/10">
                  <div className="mb-4">
                    {result.label === 'AI' ? (
                      <div className="w-16 h-16 mx-auto bg-orange-500/20 rounded-full flex items-center justify-center border border-orange-500/30">
                        <Cpu className="w-8 h-8 text-orange-400" />
                      </div>
                    ) : (
                      <div className="w-16 h-16 mx-auto bg-blue-500/20 rounded-full flex items-center justify-center border border-blue-500/30">
                        <User className="w-8 h-8 text-blue-400" />
                      </div>
                    )}
                  </div>
                  <h2 className={cn(
                    "font-sans font-black text-5xl tracking-tighter mb-1",
                    result.label === 'AI' ? "text-orange-400" : "text-blue-400"
                  )}>
                    {result.label === 'AI' ? 'Genere par IA' : 'Humain'}
                  </h2>
                  <p className="font-mono text-[10px] opacity-40 uppercase tracking-widest mt-2">Classification estimee</p>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                    <p className="font-mono text-[9px] opacity-40 uppercase mb-2">Score IA</p>
                    <p className="font-sans font-black text-2xl text-white">{(result.confidence * 100).toFixed(1)}%</p>
                    <p className="font-mono text-[8px] opacity-30 uppercase mt-1">0 humain / 1 IA</p>
                  </div>
                  <div className="bg-white/5 p-4 rounded-xl border border-white/5">
                    <p className="font-mono text-[9px] opacity-40 uppercase mb-2">Temps d'analyse</p>
                    <p className="font-sans font-black text-2xl text-white">{result.latency}ms</p>
                  </div>
                </div>

                <div className="flex-1">
                  <p className="font-mono text-[9px] opacity-40 uppercase mb-3 flex items-center gap-2">
                    <Terminal className="w-3 h-3 text-indigo-400" /> Indice principal
                  </p>
                  <p className="text-xs leading-relaxed opacity-70 font-medium italic">
                    "{result.reasoning}"
                  </p>
                </div>

                <div className="pt-4 mt-auto">
                  <button 
                    onClick={() => setResult(null)}
                    className="w-full py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 transition-all text-[10px] uppercase font-bold tracking-widest rounded-lg text-white/60 hover:text-white"
                  >
                    Effacer
                  </button>
                </div>
              </motion.div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center space-y-6 opacity-20">
                <div className="w-20 h-20 border-2 border-dashed border-white/20 rounded-full flex items-center justify-center">
                  <Activity className="w-10 h-10" />
                </div>
                <div>
                  <p className="font-sans font-bold text-sm uppercase tracking-widest text-white">Texte attendu</p>
                  <p className="text-[10px] uppercase font-mono mt-2">Pret pour une nouvelle analyse...</p>
                </div>
              </div>
            )}
          </div>

          <div className="glass p-6">
            <h4 className="font-sans font-bold text-xs uppercase tracking-widest mb-4 flex items-center gap-2 text-white/60">
              <CheckCircle2 className="w-4 h-4 text-indigo-400" /> Confidentialite
            </h4>
            <div className="space-y-3">
              {[
                { name: "Texte analyse", status: "A la demande" },
                { name: "Historique", status: "Session" },
                { name: "Donnees saisies", status: "Locales" }
              ].map((item) => (
                <div key={item.name} className="flex items-center justify-between">
                  <span className="text-[10px] font-mono text-white/50">{item.name}</span>
                  <span className="text-[10px] font-mono font-bold text-indigo-400 uppercase tracking-tighter">{item.status}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}


