import { useState } from 'react';
import MainAnalyzer from './components/MainAnalyzer';
import RollSimulator from './components/RollSimulator';
import { Button } from './components/ui/button';

type Tab = 'analyzer' | 'simulator';

function App() {
  const [activeTab, setActiveTab] = useState<Tab>('analyzer');

  return (
    <div className="h-screen w-screen bg-background text-foreground flex flex-col">
      {/* Navigation Tabs */}
      <div className="border-b bg-card">
        <div className="flex gap-2 p-2">
          <Button
            variant={activeTab === 'analyzer' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('analyzer')}
            className="flex-1"
          >
            Analyzer
          </Button>
          <Button
            variant={activeTab === 'simulator' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('simulator')}
            className="flex-1"
          >
            Simulator
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'analyzer' && <MainAnalyzer />}
        {activeTab === 'simulator' && <RollSimulator />}
      </div>
    </div>
  );
}

export default App;
