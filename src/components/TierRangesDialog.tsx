import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

const TIER_COLORS: Record<string, string> = {
  S: 'text-green-500',
  A: 'text-blue-500',
  B: 'text-cyan-500',
  C: 'text-yellow-500',
  D: 'text-orange-500',
  E: 'text-red-400',
  F: 'text-red-600',
};

const TIER_RANGES = [
  {
    tier: 'S',
    ore: '84.5 - 89.7 m³/s',
    modulated: '109.4 - 121.0+ m³/s',
    ice: '86.0 - 91.4 m³/s',
  },
  {
    tier: 'A',
    ore: '79.2 - 84.5 m³/s',
    modulated: '97.8 - 109.4 m³/s',
    ice: '80.6 - 86.0 m³/s',
  },
  {
    tier: 'B',
    ore: '73.9 - 79.2 m³/s',
    modulated: '86.2 - 97.8 m³/s',
    ice: '75.2 - 80.6 m³/s',
  },
  {
    tier: 'C',
    ore: '68.6 - 73.9 m³/s',
    modulated: '74.6 - 86.2 m³/s',
    ice: '70.0 - 75.2 m³/s',
  },
  {
    tier: 'D',
    ore: '63.4 - 68.6 m³/s',
    modulated: '63.1 - 74.6 m³/s',
    ice: '64.6 - 70.0 m³/s',
  },
  {
    tier: 'E',
    ore: '58.0 - 63.4 m³/s',
    modulated: '51.5 - 63.1 m³/s',
    ice: '59.1 - 64.6 m³/s',
  },
  {
    tier: 'F',
    ore: '< 58.0 m³/s',
    modulated: '< 51.5 m³/s',
    ice: '< 59.1 m³/s',
  },
];

interface TierRangesDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function TierRangesDialog({
  open,
  onOpenChange,
}: Readonly<TierRangesDialogProps>) {
  const getTierColor = (tier: string) => {
    return TIER_COLORS[tier] || 'text-foreground';
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Tier Ranges</DialogTitle>
          <DialogDescription>
            Reference table showing the m³/s ranges for each tier across different miner types.
          </DialogDescription>
        </DialogHeader>

        <div className="py-4">
          <div className="overflow-x-auto">
    <table className="w-full border-collapse font-mono text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-3 font-semibold">Tier</th>
                  <th className="text-right p-3 font-semibold">ORE Strip Miner</th>
                  <th className="text-right p-3 font-semibold">
        Modulated Strip Miner II
      </th>
      <th className="text-right p-3 font-semibold">
        ORE Ice Harvester
      </th>
                </tr>
              </thead>
              <tbody>
                {TIER_RANGES.map((range) => (
                  <tr key={range.tier} className="border-b">
                    <td className={`p-3 font-semibold ${getTierColor(range.tier)}`}>
                      {range.tier}
                    </td>
                    <td className="text-right p-3">{range.ore}</td>
    <td className="text-right p-3">
      {range.modulated}
    </td>
    <td className="text-right p-3">
      {range.ice}
    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
