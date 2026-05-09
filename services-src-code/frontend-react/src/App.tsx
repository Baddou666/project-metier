/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from "react";
import Navbar from "./components/layout/Navbar";
import TextAnalyzer from "./components/detector/TextAnalyzer";
import MonitoringDashboard from "./components/dashboard/MonitoringDashboard";
import { DetectionResult } from "./services/localDetections";
import { motion, AnimatePresence } from "motion/react";

export default function App() {
  const [activeTab, setActiveTab] = useState<'detector' | 'dashboard'>('detector');
  const [selectedDetection, setSelectedDetection] = useState<DetectionResult | null>(null);

  const handleSelectDetection = (detection: DetectionResult) => {
    setSelectedDetection(detection);
    setActiveTab('detector');
  };

  return (
    <div className="min-h-screen text-white font-sans selection:bg-indigo-500 selection:text-white">
      <div className="mesh-bg" />
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
      />

      <main className="relative z-10">
        <AnimatePresence mode="wait">
          {activeTab === 'detector' ? (
            <motion.div
              key="detector"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3 }}
            >
              <TextAnalyzer selectedDetection={selectedDetection} />
            </motion.div>
          ) : (
            <motion.div
              key="dashboard"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3 }}
            >
              <MonitoringDashboard onSelectDetection={handleSelectDetection} />
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      <footer className="relative z-10 py-8 border-t border-white/10 bg-white/5 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4 text-[10px] font-mono text-white/40 uppercase tracking-widest">
            <span>2026 AI Text Detector</span>
            <span className="hidden md:inline">-</span>
            <span>Analyse de texte</span>
          </div>
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 rounded-full bg-green-500" />
              <span className="text-[10px] font-mono opacity-50 uppercase tracking-widest">Analyse disponible</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 rounded-full bg-blue-500" />
              <span className="text-[10px] font-mono opacity-50 uppercase tracking-widest">Donnees de session</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
