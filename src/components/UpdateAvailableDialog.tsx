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
import { Download, XIcon } from 'lucide-react';
import type { VersionCheckResult } from '@/lib/utils/versionCheck';

interface UpdateAvailableDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  versionCheck: VersionCheckResult;
}

export default function UpdateAvailableDialog({
  open,
  onOpenChange,
  versionCheck,
}: Readonly<UpdateAvailableDialogProps>) {
  const handleDownload = async () => {
    if (versionCheck.updateUrl) {
      await openUrl(versionCheck.updateUrl);
    }
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Download className="h-5 w-5 text-primary" />
            Update Available!
          </DialogTitle>
          <DialogDescription>
            A new version of the app is available for download.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3 py-4">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Current Version:</span>
            <span className="font-mono font-semibold">v{versionCheck.currentVersion}</span>
          </div>
          {versionCheck.latestVersion && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Latest Version:</span>
              <span className="font-mono font-semibold text-primary">v{versionCheck.latestVersion}</span>
            </div>
          )}
        </div>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="w-full sm:w-auto"
          >
            <XIcon className="h-4 w-4 mr-2" />
            Later
          </Button>
          <Button
            onClick={handleDownload}
            className="w-full sm:w-auto"
          >
            <Download className="h-4 w-4 mr-2" />
            Download Update
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
