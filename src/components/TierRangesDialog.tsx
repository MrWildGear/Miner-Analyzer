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
    modulated: '3.76188 - 3.97+ m³/s',
    ice: '7.033 - 7.44+ m³/s',
  },
  {
    tier: 'A',
    ore: '79.2 - 84.5 m³/s',
    modulated: '3.55376 - 3.76188 m³/s',
    ice: '6.627 - 7.033 m³/s',
  },
  {
    tier: 'B',
    ore: '73.9 - 79.2 m³/s',
    modulated: '3.34564 - 3.55376 m³/s',
    ice: '6.220 - 6.627 m³/s',
  },
  {
    tier: 'C',
    ore: '68.6 - 73.9 m³/s',
    modulated: '3.13752 - 3.34564 m³/s',
    ice: '5.813 - 6.220 m³/s',
  },
  {
    tier: 'D',
    ore: '63.4 - 68.6 m³/s',
    modulated: '2.92940 - 3.13752 m³/s',
    ice: '5.407 - 5.813 m³/s',
  },
  {
    tier: 'E',
    ore: '58.0 - 63.4 m³/s',
    modulated: '2.67 - 2.92940 m³/s',
    ice: '5.000 - 5.407 m³/s',
  },
  {
    tier: 'F',
    ore: '< 58.0 m³/s',
    modulated: '< 2.67 m³/s',
    ice: '< 5.000 m³/s',
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
      <th className="text-right p-3 font-semibold line-through opacity-60">
        Modulated Strip Miner II
      </th>
      <th className="text-right p-3 font-semibold line-through opacity-60">
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
    <td className="text-right p-3 line-through opacity-60">
      {range.modulated}
    </td>
    <td className="text-right p-3 line-through opacity-60">
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
