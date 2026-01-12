import { useState, useEffect } from 'react';
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

interface ExportFormatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  format: string;
  onSave: (format: string) => void;
}

const AVAILABLE_PLACEHOLDERS = [
  { value: '{tier}', label: 'Tier (A, B, C, etc.)' },
  { value: '{m3Pct}', label: 'Base M3/sec % change' },
  { value: '{effectiveM3Pct}', label: 'Effective M3/sec % change' },
  { value: '{m3PerSec}', label: 'Base M3/sec value' },
  { value: '{effectiveM3PerSec}', label: 'Effective M3/sec value' },
  { value: '{optimalRangePct}', label: 'Optimal Range % change' },
  { value: '{optimalRange}', label: 'Optimal Range value' },
  { value: '{minerType}', label: 'Miner Type (ORE/Modulated/Ice)' },
  { value: '{MiningAmount}', label: 'Mining Amount' },
  { value: '{ActivationTime}', label: 'Activation Time' },
  { value: '{CriticalSuccessChance}', label: 'Crit Chance' },
  { value: '{CriticalSuccessBonusYield}', label: 'Crit Bonus' },
  { value: '{ResidueProbability}', label: 'Residue Probability' },
  { value: '{ResidueVolumeMultiplier}', label: 'Residue Volume Multiplier' },
];

const DEFAULT_FORMAT = '{tier} : ({m3Pct}%) {optimalRangePct} [{minerType}]';

// Note: The default format matches the original behavior where:
// - Tier has a space after it (unless it ends with '+')
// - Base M3/sec percentage is shown
// - Optimal Range percentage is shown if available
// - Miner type is shown in brackets

export default function ExportFormatDialog({
  open,
  onOpenChange,
  format,
  onSave,
}: Readonly<ExportFormatDialogProps>) {
  const [localFormat, setLocalFormat] = useState(format);
  const [error, setError] = useState<string>('');
  const [preview, setPreview] = useState<string>('');
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const [cursorPosition, setCursorPosition] = useState<number | null>(null);

  useEffect(() => {
    if (open) {
      setLocalFormat(format);
      setError('');
      // Generate preview with sample data
      const samplePreview = format
        .replace(/{tier}/g, 'A')
        .replace(/{m3Pct}/g, '+34.4')
        .replace(/{effectiveM3Pct}/g, '+35.2')
        .replace(/{m3PerSec}/g, '12.5')
        .replace(/{effectiveM3PerSec}/g, '13.2')
        .replace(/{optimalRangePct}/g, '{+05.2%}')
        .replace(/{optimalRange}/g, '19.5')
        .replace(/{minerType}/g, 'ORE')
        .replace(/{MiningAmount}/g, '210')
        .replace(/{ActivationTime}/g, '43')
        .replace(/{CriticalSuccessChance}/g, '0.012')
        .replace(/{CriticalSuccessBonusYield}/g, '2.1')
        .replace(/{ResidueProbability}/g, '0.015')
        .replace(/{ResidueVolumeMultiplier}/g, '0.5');
      setPreview(samplePreview);
    }
  }, [open, format]);

  const handleFormatChange = (value: string) => {
    setLocalFormat(value);
    // Validate length
    const sampleLength = value
      .replace(/{tier}/g, 'A')
      .replace(/{m3Pct}/g, '+34.4')
      .replace(/{effectiveM3Pct}/g, '+35.2')
      .replace(/{m3PerSec}/g, '12.5')
      .replace(/{effectiveM3PerSec}/g, '13.2')
      .replace(/{optimalRangePct}/g, '{+05.2%}')
      .replace(/{optimalRange}/g, '19.5')
      .replace(/{minerType}/g, 'ORE')
      .replace(/{MiningAmount}/g, '210')
        .replace(/{ActivationTime}/g, '43')
        .replace(/{CriticalSuccessChance}/g, '0.012')
        .replace(/{CriticalSuccessBonusYield}/g, '2.1')
        .replace(/{ResidueProbability}/g, '0.015')
        .replace(/{ResidueVolumeMultiplier}/g, '0.5').length;

    if (sampleLength > 100) {
      setError(`Format too long (${sampleLength} chars, max 100)`);
    } else {
      setError('');
    }

    // Update preview
    const samplePreview = value
      .replace(/{tier}/g, 'A')
      .replace(/{m3Pct}/g, '+34.4')
      .replace(/{effectiveM3Pct}/g, '+35.2')
      .replace(/{m3PerSec}/g, '12.5')
      .replace(/{effectiveM3PerSec}/g, '13.2')
      .replace(/{optimalRangePct}/g, '{+05.2%}')
      .replace(/{optimalRange}/g, '19.5')
      .replace(/{minerType}/g, 'ORE')
      .replace(/{MiningAmount}/g, '210')
      .replace(/{ActivationTime}/g, '43')
      .replace(/{CriticalSuccessChance}/g, '0.012')
      .replace(/{CriticalSuccessBonusYield}/g, '2.1')
      .replace(/{ResidueProbability}/g, '0.015')
      .replace(/{ResidueVolumeMultiplier}/g, '0.5');
    setPreview(samplePreview);
  };

  const handleInsertPlaceholder = (placeholder: string, position?: number) => {
    if (position !== null && position !== undefined) {
      // Insert at cursor position
      const before = localFormat.substring(0, position);
      const after = localFormat.substring(position);
      const newFormat = before + placeholder + after;
      handleFormatChange(newFormat);
      // Restore cursor position after placeholder
      setTimeout(() => {
        const input = document.getElementById('export-format') as HTMLInputElement;
        if (input) {
          input.setSelectionRange(position + placeholder.length, position + placeholder.length);
        }
      }, 0);
    } else {
      // Append at end
      const newFormat = localFormat + placeholder;
      handleFormatChange(newFormat);
    }
  };

  const handleDragStart = (e: React.DragEvent, placeholder: string) => {
    e.dataTransfer.setData('text/plain', placeholder);
    e.dataTransfer.effectAllowed = 'copy';
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'copy';
    setIsDraggingOver(true);
  };

  const handleDragLeave = () => {
    setIsDraggingOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDraggingOver(false);
    const placeholder = e.dataTransfer.getData('text/plain');
    if (placeholder) {
      const input = e.currentTarget as HTMLInputElement;
      const position = input.selectionStart ?? localFormat.length;
      handleInsertPlaceholder(placeholder, position);
    }
  };

  const handleInputFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    setCursorPosition(e.target.selectionStart);
  };

  const handleInputSelect = (e: React.SyntheticEvent<HTMLInputElement>) => {
    const target = e.currentTarget;
    setCursorPosition(target.selectionStart);
  };

  const handleReset = () => {
    handleFormatChange(DEFAULT_FORMAT);
  };

  const handleSave = () => {
    if (error) {
      return;
    }
    onSave(localFormat);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Export Format</DialogTitle>
          <DialogDescription>
            Customize the export format. Maximum 100 characters. Use placeholders
            below.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="export-format">Format Template</Label>
            <Input
              id="export-format"
              value={localFormat}
              onChange={(e) => handleFormatChange(e.target.value)}
              onFocus={handleInputFocus}
              onSelect={handleInputSelect}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              placeholder={DEFAULT_FORMAT}
              className={isDraggingOver ? 'ring-2 ring-primary border-primary' : ''}
            />
            {error && <p className="text-sm text-destructive">{error}</p>}
            {!error && preview && (
              <p className="text-sm text-muted-foreground">
                Preview: <span className="font-mono">{preview}</span> (
                {preview.length} chars)
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Available Placeholders (Drag & Drop or Click)</Label>
            <div className="grid grid-cols-2 gap-2">
              {AVAILABLE_PLACEHOLDERS.map((placeholder) => (
                <Button
                  key={placeholder.value}
                  variant="outline"
                  size="sm"
                  draggable
                  onDragStart={(e) => handleDragStart(e, placeholder.value)}
                  onClick={() => {
                    const input = document.getElementById('export-format') as HTMLInputElement;
                    const position = input?.selectionStart ?? localFormat.length;
                    handleInsertPlaceholder(placeholder.value, position);
                  }}
                  className="justify-start text-left cursor-grab active:cursor-grabbing hover:bg-accent"
                >
                  <code className="text-xs mr-2">{placeholder.value}</code>
                  <span className="text-xs text-muted-foreground">
                    {placeholder.label}
                  </span>
                </Button>
              ))}
            </div>
          </div>

          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={handleReset}>
              Reset to Default
            </Button>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={!!error}>
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
