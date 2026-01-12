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
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';

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

const DEFAULT_FORMAT = '{tier} : {m3Pct}% {optimalRangePct} {minerType}';

// Note: The default format is minimal - users can add their own formatting
// characters like parentheses, brackets, etc. as needed.

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
        .replace(/{optimalRangePct}/g, '+05.2%')
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
      .replace(/{optimalRangePct}/g, '-04.2%')
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
      .replace(/{optimalRangePct}/g, '-04.2%')
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
        const textarea = document.getElementById('export-format') as HTMLTextAreaElement;
        if (textarea) {
          textarea.setSelectionRange(position + placeholder.length, position + placeholder.length);
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
      const textarea = e.currentTarget as HTMLTextAreaElement;
      const position = textarea.selectionStart ?? localFormat.length;
      handleInsertPlaceholder(placeholder, position);
    }
  };

  const handleInputFocus = (e: React.FocusEvent<HTMLTextAreaElement>) => {
    setCursorPosition(e.target.selectionStart);
  };

  const handleInputSelect = (e: React.SyntheticEvent<HTMLTextAreaElement>) => {
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
            <Textarea
              id="export-format"
              value={localFormat}
              onChange={(e) => handleFormatChange(e.target.value)}
              onFocus={handleInputFocus}
              onSelect={handleInputSelect}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              placeholder={DEFAULT_FORMAT}
              className={cn(
                'min-h-[108px]',
                isDraggingOver ? 'ring-2 ring-primary border-primary' : ''
              )}
              wrap="soft"
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
            <Label>Available Placeholders (Click)</Label>
            <div className="grid grid-cols-2 gap-2">
              {AVAILABLE_PLACEHOLDERS.map((placeholder) => (
                <Button
                  key={placeholder.value}
                  variant="outline"
                  draggable
                  onDragStart={(e) => handleDragStart(e, placeholder.value)}
                  onClick={() => {
                    const textarea = document.getElementById('export-format') as HTMLTextAreaElement;
                    const position = textarea?.selectionStart ?? localFormat.length;
                    handleInsertPlaceholder(placeholder.value, position);
                  }}
                  className="flex flex-col items-start h-auto min-h-[60px] py-2 px-3 text-left cursor-grab active:cursor-grabbing hover:bg-accent"
                >
                  <code className="text-sm font-medium text-blue-600 dark:text-blue-400 mb-1">
                    {placeholder.value}
                  </code>
                  <span className="text-xs text-muted-foreground leading-tight">
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
