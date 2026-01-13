import { useState, useEffect } from 'react';
import { openUrl } from '@/lib/utils/openUrl';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Download, CheckCircle2, AlertCircle, RefreshCw } from 'lucide-react';
import type { TierModifiers } from '@/types';
import { APP_VERSION } from '@/version';
import { useVersionCheck } from '@/hooks/useVersionCheck';

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rollCost: number;
  tierModifiers: TierModifiers;
  optimalRangeModifier: number;
  onSave: (
    rollCost: number,
    tierModifiers: TierModifiers,
    optimalRangeModifier: number,
  ) => void;
}

export default function SettingsDialog({
  open,
  onOpenChange,
  rollCost,
  tierModifiers,
  optimalRangeModifier,
  onSave,
}: Readonly<SettingsDialogProps>) {
  const [localRollCost, setLocalRollCost] = useState(rollCost.toString());
  const [localTierModifiers, setLocalTierModifiers] = useState<TierModifiers>({
    ...tierModifiers,
  });
  const [localOptimalRangeModifier, setLocalOptimalRangeModifier] =
    useState(optimalRangeModifier.toString());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const { result: versionCheck, isChecking: isCheckingVersion, checkForUpdates } = useVersionCheck(false);

  // Sync local state with props when dialog opens or props change
  useEffect(() => {
    if (open) {
      setLocalRollCost(rollCost.toString());
      setLocalTierModifiers({ ...tierModifiers });
      setLocalOptimalRangeModifier(optimalRangeModifier.toString());
      setErrors({});
    }
  }, [open, rollCost, tierModifiers, optimalRangeModifier]);

  const handleSave = () => {
    const newErrors: Record<string, string> = {};

    // Validate roll cost
    const costValue = Number.parseFloat(localRollCost);
    if (Number.isNaN(costValue) || costValue < 0) {
      newErrors.rollCost = 'Invalid roll cost';
    }

    // Validate tier modifiers
    const tiers: Array<keyof TierModifiers> = ['S', 'A', 'B', 'C', 'D', 'E', 'F'];
    for (const tier of tiers) {
      const value = localTierModifiers[tier];
      if (Number.isNaN(value) || value < 0) {
        newErrors[`tier_${tier}`] = `Invalid modifier for tier ${tier}`;
      }
    }

    // Validate optimal range modifier
    const optimalValue = Number.parseFloat(localOptimalRangeModifier);
    if (Number.isNaN(optimalValue) || optimalValue < 0) {
      newErrors.optimalRange = 'Invalid optimal range modifier';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    onSave(costValue, localTierModifiers, optimalValue);
  };

  const handleTierModifierChange = (
    tier: keyof TierModifiers,
    value: string,
  ) => {
    const numValue = Number.parseFloat(value);
    if (!Number.isNaN(numValue)) {
      setLocalTierModifiers({
        ...localTierModifiers,
        [tier]: numValue,
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Settings</DialogTitle>
          <DialogDescription>
            Configure roll cost and tier modifiers for sell price calculations.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="roll-cost">Roll Cost (ISK)</Label>
            <Input
              id="roll-cost"
              type="number"
              value={localRollCost}
              onChange={(e) => setLocalRollCost(e.target.value)}
              min="0"
            />
            {errors.rollCost && (
              <p className="text-sm text-destructive">{errors.rollCost}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Tier Modifiers</Label>
            <div className="grid grid-cols-2 gap-2">
              {(['S', 'A', 'B', 'C', 'D', 'E', 'F'] as Array<
                keyof TierModifiers
              >).map((tier) => (
                <div key={tier} className="space-y-1">
                  <Label htmlFor={`tier-${tier}`} className="text-xs">
                    Tier {tier}:
                  </Label>
                  <Input
                    id={`tier-${tier}`}
                    type="number"
                    value={localTierModifiers[tier]}
                    onChange={(e) =>
                      handleTierModifierChange(tier, e.target.value)
                    }
                    min="0"
                    step="0.1"
                  />
                  {errors[`tier_${tier}`] && (
                    <p className="text-xs text-destructive">
                      {errors[`tier_${tier}`]}
                    </p>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="optimal-range">
              Optimal Range Modifier (applies when tier has '+')
            </Label>
            <Input
              id="optimal-range"
              type="number"
              value={localOptimalRangeModifier}
              onChange={(e) => setLocalOptimalRangeModifier(e.target.value)}
              min="0"
              step="0.1"
            />
            {errors.optimalRange && (
              <p className="text-sm text-destructive">
                {errors.optimalRange}
              </p>
            )}
          </div>

          <div className="space-y-2 pt-4 border-t">
            <Label>Version Information</Label>
            <div className="space-y-2 text-sm">
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">Current Version:</span>
                <span className="font-mono">v{APP_VERSION}</span>
              </div>
              {versionCheck && (
                <>
                  {versionCheck.latestVersion && (
                    <div className="flex items-center justify-between">
                      <span className="text-muted-foreground">Latest Version:</span>
                      <span className="font-mono">v{versionCheck.latestVersion}</span>
                    </div>
                  )}
                  <div className="flex items-center gap-2 pt-2">
                    {!versionCheck.isUpToDate && versionCheck.latestVersion && (
                      <Button
                        variant="default"
                        size="sm"
                        onClick={async () => {
                          if (versionCheck.updateUrl) {
                            await openUrl(versionCheck.updateUrl);
                          }
                        }}
                        className="flex-1"
                      >
                        <Download className="h-4 w-4 mr-2" />
                        Download Update
                      </Button>
                    )}
                    {versionCheck.isUpToDate && versionCheck.latestVersion && (
                      <div className="flex items-center gap-2 text-sm text-green-600 dark:text-green-400 flex-1 justify-center">
                        <CheckCircle2 className="h-4 w-4" />
                        Up to date
                      </div>
                    )}
                    {versionCheck.error && (
                      <div className="flex items-center gap-2 text-sm text-yellow-600 dark:text-yellow-400 flex-1 justify-center">
                        <AlertCircle className="h-4 w-4" />
                        {versionCheck.error}
                      </div>
                    )}
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={checkForUpdates}
                      disabled={isCheckingVersion}
                    >
                      <RefreshCw className={`h-4 w-4 ${isCheckingVersion ? 'animate-spin' : ''}`} />
                    </Button>
                  </div>
                </>
              )}
              {!versionCheck && !isCheckingVersion && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={checkForUpdates}
                  className="w-full"
                >
                  Check for Updates
                </Button>
              )}
              {isCheckingVersion && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground justify-center py-2">
                  <RefreshCw className="h-4 w-4 animate-spin" />
                  Checking for updates...
                </div>
              )}
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSave}>Save</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
