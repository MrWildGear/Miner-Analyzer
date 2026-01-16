import { useState, useEffect } from 'react';
import type { SkillLevels } from '@/types';
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

interface SkillLevelsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  skillLevels: SkillLevels;
  onSave: (skillLevels: SkillLevels) => void;
}

const SKILL_FIELDS: Array<{
  key: keyof SkillLevels;
  label: string;
}> = [
  { key: 'mining', label: 'Mining' },
  { key: 'astrogeology', label: 'Astrogeology' },
  { key: 'miningBarge', label: 'Mining Barge' },
  { key: 'exhumers', label: 'Exhumers' },
  { key: 'miningExploitation', label: 'Mining Exploitation' },
  { key: 'miningPrecision', label: 'Mining Precision' },
  { key: 'iceHarvesting', label: 'Ice Harvesting' },
  { key: 'iceHarvestingImplant', label: 'Ice Harvesting Implant %' },
];

export default function SkillLevelsDialog({
  open,
  onOpenChange,
  skillLevels,
  onSave,
}: Readonly<SkillLevelsDialogProps>) {
  const [localSkills, setLocalSkills] = useState<SkillLevels>(skillLevels);

  useEffect(() => {
    if (open) {
      setLocalSkills(skillLevels);
    }
  }, [open, skillLevels]);

  const handleSkillChange = (key: keyof SkillLevels, value: string) => {
    const parsed = Number.parseInt(value, 10);
    if (Number.isNaN(parsed)) {
      return;
    }
    const clamped = Math.min(5, Math.max(0, parsed));
    setLocalSkills((prev) => ({
      ...prev,
      [key]: clamped,
    }));
  };

  const handleSave = () => {
    onSave(localSkills);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Skill Levels</DialogTitle>
          <DialogDescription>
            Set mining-related skills (0-5). Ice implant is a percent (0-5).
          </DialogDescription>
        </DialogHeader>

        <div className="grid grid-cols-2 gap-3 py-4">
          {SKILL_FIELDS.map((field) => (
            <div key={field.key} className="space-y-1">
              <Label htmlFor={`skill-${field.key}`} className="text-xs">
                {field.label}
              </Label>
              <Input
                id={`skill-${field.key}`}
                type="number"
                min="0"
                max="5"
                step="1"
                value={localSkills[field.key]}
                onChange={(e) => handleSkillChange(field.key, e.target.value)}
              />
            </div>
          ))}
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
