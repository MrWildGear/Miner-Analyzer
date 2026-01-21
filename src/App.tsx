import { useState } from 'react';
import MainAnalyzer from './components/MainAnalyzer';
import RollSimulator from './components/RollSimulator';
import LogView from './components/LogView';
import { Button } from './components/ui/button';

type Tab = 'analyzer' | 'simulator' | 'logview';

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
          <Button
            variant={activeTab === 'logview' ? 'default' : 'ghost'}
            onClick={() => setActiveTab('logview')}
            className="flex-1"
          >
            Log View
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'analyzer' && <MainAnalyzer />}
        {activeTab === 'simulator' && <RollSimulator />}
        {activeTab === 'logview' && <LogView />}
      </div>
    </div>
  );
}

export default App;
