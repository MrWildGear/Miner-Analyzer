# EVE Online - Strip Miner Mutaplasmid Odds Calculator
# Calculates probability of achieving target Effective Mining Speed

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  ORE Strip Miner Mutaplasmid Probability Calculator" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

# Base stats
$baseMining = 200.0
$baseCycle = 45.0
$baseCritChance = 0.01  # 1%
$baseCritBonus = 2.0     # 200% bonus = 2x multiplier

# Mutation ranges
$miningMin = -0.15
$miningMax = 0.30
$cycleMin = -0.10
$cycleMax = 0.10
$critChanceMin = -0.35
$critChanceMax = 0.30
$critBonusMin = -0.20
$critBonusMax = 0.15

# Tier thresholds for target selection
$tierThresholds = @{
    'S' = 6.27
    'A' = 5.92
    'B' = 5.57
    'C' = 5.23
    'D' = 4.88
    'E' = 4.44
    'F' = 0.0
}

# Number of iterations
Write-Host ""
Write-Host "SIMULATION SETTINGS:" -ForegroundColor Cyan
Write-Host "Select target tier for INVESTMENT ANALYSIS (S/A/B/C/D/E/F)" -ForegroundColor Yellow
Write-Host "S: >=6.27  A: 5.92-6.27  B: 5.57-5.92  C: 5.23-5.57  D: 4.88-5.23  E: 4.44-4.88  F: <4.44" -ForegroundColor Gray
$targetTierInput = Read-Host "Target tier (default: A)"
if ([string]::IsNullOrWhiteSpace($targetTierInput)) {
    $targetTier = 'A'
} else {
    $targetTier = $targetTierInput.Trim().ToUpper()
    if (-not $tierThresholds.ContainsKey($targetTier)) {
        Write-Host "Invalid tier. Using default: A" -ForegroundColor Yellow
        $targetTier = 'A'
    }
}
$targetSpeed = $tierThresholds[$targetTier]
Write-Host "Target tier: $targetTier ($targetSpeed m³/s)" -ForegroundColor Green
Write-Host ""

Write-Host "SIMULATION SETTINGS (continued):" -ForegroundColor Cyan
Write-Host "Enter number of rolls to simulate (default: 1,000,000)" -ForegroundColor Yellow
Write-Host "Examples: 1m (1 million), 5m (5 million), 1b (1 billion), 100k (100,000)" -ForegroundColor Gray
Write-Host "Press ENTER for default" -ForegroundColor Gray
$iterInput = Read-Host "Number of rolls"
if ([string]::IsNullOrWhiteSpace($iterInput)) {
    $iterations = 1000000
    Write-Host "Using default: 1,000,000 rolls" -ForegroundColor Gray
} else {
    try {
        # Parse shorthand notation (k, m, b)
        $iterInput = $iterInput.Trim().ToLower()
        if ($iterInput -match '^(\d+\.?\d*)\s*([kmb])$') {
            $number = [double]$matches[1]
            $multiplier = switch ($matches[2]) {
                'k' { 1000 }
                'm' { 1000000 }
                'b' { 1000000000 }
            }
            $iterations = [int]($number * $multiplier)
        } else {
            $iterations = [int]$iterInput
        }
        
        if ($iterations -lt 1000) {
            Write-Host "Minimum 1,000 rolls. Setting to 1,000." -ForegroundColor Yellow
            $iterations = 1000
        } elseif ($iterations -gt 2000000000) {
            Write-Host "Maximum 2 billion rolls. Setting to 2 billion." -ForegroundColor Yellow
            $iterations = 2000000000
        }
        Write-Host "Simulating $($iterations.ToString('N0')) rolls" -ForegroundColor Green
    } catch {
        Write-Host "Invalid input. Using default: 1,000,000 rolls" -ForegroundColor Yellow
        $iterations = 1000000
    }
}
Write-Host ""
Write-Host "TIP: Press Ctrl+C during simulation to stop early and see partial results" -ForegroundColor DarkGray
Write-Host ""

Write-Host "Base Stats:" -ForegroundColor Yellow
Write-Host "  Mining Amount: $baseMining m³"
Write-Host "  Cycle Time: $baseCycle s"
Write-Host "  Crit Chance: $($baseCritChance * 100)%"
Write-Host "  Crit Bonus Multiplier: $($baseCritBonus)x"
$baseSpeed = ($baseMining * (1 + $baseCritChance * $baseCritBonus)) / $baseCycle
Write-Host "  Base Effective Speed: $([math]::Round($baseSpeed, 2)) m³/s" -ForegroundColor Green
Write-Host ""

Write-Host "Mutation Ranges:" -ForegroundColor Yellow
Write-Host "  Mining Amount: $($miningMin * 100)% to +$($miningMax * 100)%"
Write-Host "  Cycle Time: $($cycleMin * 100)% to +$($cycleMax * 100)%"
Write-Host "  Crit Chance: $($critChanceMin * 100)% to +$($critChanceMax * 100)%"
Write-Host "  Crit Bonus: $($critBonusMin * 100)% to +$($critBonusMax * 100)%"
Write-Host ""

# Function to calculate effective mining speed
function Get-EffectiveMiningSpeed {
    param(
        [double]$miningRoll,
        [double]$cycleRoll,
        [double]$critChanceRoll,
        [double]$critBonusRoll
    )
    
    $mining = $baseMining * (1 + $miningRoll)
    $cycle = $baseCycle * (1 + $cycleRoll)
    $critChance = $baseCritChance * (1 + $critChanceRoll)
    $critMult = $baseCritBonus * (1 + $critBonusRoll)
    
    $grossYield = $mining * (1 + $critChance * $critMult)
    $speed = $grossYield / $cycle
    
    return $speed
}

# Monte Carlo Simulation (Multithreaded using Runspaces with Live Feedback)
Write-Host "Running Monte Carlo Simulation (Multithreaded)..." -ForegroundColor Cyan

# Determine optimal thread count
$threadCount = [Environment]::ProcessorCount
Write-Host "Using $threadCount threads (Runspace Pool)..." -ForegroundColor Yellow
Write-Host ""

# Create synchronized hashtable for progress tracking
$sync = [hashtable]::Synchronized(@{
    CompletedRolls = 0
    SRolls = 0
    ARolls = 0
    BRolls = 0
    CRolls = 0
    DRolls = 0
    ERolls = 0
    FRolls = 0
})

# Create runspace pool for parallel execution
$runspacePool = [runspacefactory]::CreateRunspacePool(1, $threadCount)
$runspacePool.Open()

# Calculate speed bounds for histogram (worst and best case scenarios)
# Worst case: all negative mutations
$worstMining = $baseMining * (1 + $miningMin)
$worstCycle = $baseCycle * (1 + $cycleMax)
$worstCritChance = [Math]::Max(0, $baseCritChance * (1 + $critChanceMin))
$worstCritBonus = $baseCritBonus * (1 + $critBonusMin)
$worstYield = $worstMining * (1 + $worstCritChance * $worstCritBonus)
$histMin = ($worstYield / $worstCycle) * 0.95  # Add 5% margin

# Best case: all positive mutations
$bestMining = $baseMining * (1 + $miningMax)
$bestCycle = $baseCycle * (1 + $cycleMin)
$bestCritChance = $baseCritChance * (1 + $critChanceMax)
$bestCritBonus = $baseCritBonus * (1 + $critBonusMax)
$bestYield = $bestMining * (1 + $bestCritChance * $bestCritBonus)
$histMax = ($bestYield / $bestCycle) * 1.05  # Add 5% margin

# Use 5000 bins for good percentile accuracy (small memory footprint: ~20KB)
$histBins = 5000

# Split iterations into batches
$batchSize = [math]::Ceiling($iterations / $threadCount)
$jobs = @()

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Define the script block for each thread
$scriptBlock = {
    param($batchCount, $baseMining, $baseCycle, $baseCritChance, $baseCritBonus, 
          $miningMin, $miningMax, $cycleMin, $cycleMax, 
          $critChanceMin, $critChanceMax, $critBonusMin, $critBonusMax, $targetSpeed, $syncHash,
          $histMin, $histMax, $histBins)
    
    # Use System.Random for better performance (faster than Get-Random)
    $rng = [System.Random]::new([System.Environment]::TickCount + [System.Threading.Thread]::CurrentThread.ManagedThreadId)
    
    $batchSuccessCount = 0
    $updateInterval = 1000  # Update progress every 1000 rolls
    $localCounter = 0
    
    # Pre-calculate ranges for performance
    $miningRange = $miningMax - $miningMin
    $cycleRange = $cycleMax - $cycleMin
    $critChanceRange = $critChanceMax - $critChanceMin
    $critBonusRange = $critBonusMax - $critBonusMin
    
    # Incremental statistics tracking
    $localMin = [double]::MaxValue
    $localMax = [double]::MinValue
    $localSum = 0.0
    $localCount = 0
    
    # Histogram for percentile calculation (memory efficient)
    $histogram = New-Object int[] $histBins
    $histBinWidth = ($histMax - $histMin) / $histBins
    
    # Local counters for quality tiers
    $localSRolls = 0
    $localARolls = 0
    $localBRolls = 0
    $localCRolls = 0
    $localDRolls = 0
    $localERolls = 0
    $localFRolls = 0
    
    for ($i = 0; $i -lt $batchCount; $i++) {
        # Generate random rolls using System.Random (much faster)
        $miningRoll = $miningMin + $rng.NextDouble() * $miningRange
        $cycleRoll = $cycleMin + $rng.NextDouble() * $cycleRange
        $critChanceRoll = $critChanceMin + $rng.NextDouble() * $critChanceRange
        $critBonusRoll = $critBonusMin + $rng.NextDouble() * $critBonusRange
        
        # Calculate speed inline (for performance)
        $mining = $baseMining * (1 + $miningRoll)
        $cycle = $baseCycle * (1 + $cycleRoll)
        $critChance = $baseCritChance * (1 + $critChanceRoll)
        $critMult = $baseCritBonus * (1 + $critBonusRoll)
        $grossYield = $mining * (1 + $critChance * $critMult)
        $speed = $grossYield / $cycle
        
        # Update incremental statistics (no array storage needed!)
        if ($speed -lt $localMin) { $localMin = $speed }
        if ($speed -gt $localMax) { $localMax = $speed }
        $localSum += $speed
        $localCount++
        
        # Update histogram for percentile calculation
        $binIndex = [Math]::Floor(($speed - $histMin) / $histBinWidth)
        if ($binIndex -lt 0) { $binIndex = 0 }
        elseif ($binIndex -ge $histBins) { $binIndex = $histBins - 1 }
        $histogram[$binIndex]++
        
        if ($speed -ge $targetSpeed) {
            $batchSuccessCount++
        }
        
        # Categorize the roll
        if ($speed -ge 6.27) { $localSRolls++ }
        elseif ($speed -ge 5.92) { $localARolls++ }
        elseif ($speed -ge 5.57) { $localBRolls++ }
        elseif ($speed -ge 5.23) { $localCRolls++ }
        elseif ($speed -ge 4.88) { $localDRolls++ }
        elseif ($speed -ge 4.44) { $localERolls++ }
        else { $localFRolls++ }
        
        $localCounter++
        
        # Update shared progress periodically (not every iteration for performance)
        if ($localCounter -ge $updateInterval) {
            $syncHash.CompletedRolls += $localCounter
            $syncHash.SRolls += $localSRolls
            $syncHash.ARolls += $localARolls
            $syncHash.BRolls += $localBRolls
            $syncHash.CRolls += $localCRolls
            $syncHash.DRolls += $localDRolls
            $syncHash.ERolls += $localERolls
            $syncHash.FRolls += $localFRolls
            
            $localCounter = 0
            $localSRolls = 0
            $localARolls = 0
            $localBRolls = 0
            $localCRolls = 0
            $localDRolls = 0
            $localERolls = 0
            $localFRolls = 0
        }
    }
    
    # Final update for remaining rolls
    if ($localCounter -gt 0) {
        $syncHash.CompletedRolls += $localCounter
        $syncHash.SRolls += $localSRolls
        $syncHash.ARolls += $localARolls
        $syncHash.BRolls += $localBRolls
        $syncHash.CRolls += $localCRolls
        $syncHash.DRolls += $localDRolls
        $syncHash.ERolls += $localERolls
        $syncHash.FRolls += $localFRolls
    }
    
    # Return batch results (no large array, just statistics!)
    return @{
        SuccessCount = $batchSuccessCount
        Min = $localMin
        Max = $localMax
        Sum = $localSum
        Count = $localCount
        Histogram = $histogram
    }
}

# Launch threads
for ($i = 0; $i -lt $threadCount; $i++) {
    $start = $i * $batchSize
    $end = [math]::Min(($i + 1) * $batchSize, $iterations)
    $count = $end - $start
    
    $powershell = [powershell]::Create().AddScript($scriptBlock).AddArgument($count).AddArgument($baseMining).AddArgument($baseCycle).AddArgument($baseCritChance).AddArgument($baseCritBonus).AddArgument($miningMin).AddArgument($miningMax).AddArgument($cycleMin).AddArgument($cycleMax).AddArgument($critChanceMin).AddArgument($critChanceMax).AddArgument($critBonusMin).AddArgument($critBonusMax).AddArgument($targetSpeed).AddArgument($sync).AddArgument($histMin).AddArgument($histMax).AddArgument($histBins)
    
    $powershell.RunspacePool = $runspacePool
    
    $jobs += [PSCustomObject]@{
        PowerShell = $powershell
        Handle = $powershell.BeginInvoke()
    }
}

# Monitor progress with live statistics (in-place updates)
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""
Write-Host ""

$cursorTop = [Console]::CursorTop - 10

# Set up Ctrl+C handler
$ctrlCPressed = $false
$null = [Console]::TreatControlCAsInput = $false
$cancelScript = {
    $script:ctrlCPressed = $true
}
try {
    $null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action $cancelScript
} catch {
    # Registration may fail in some environments, continue anyway
}

while (($jobs | Where-Object { -not $_.Handle.IsCompleted }).Count -gt 0 -and -not $ctrlCPressed) {
    $completed = $sync.CompletedRolls
    $percent = [math]::Round(($completed / $iterations) * 100, 1)
    
    try {
        [Console]::SetCursorPosition(0, $cursorTop)
    } catch {
        # Cursor positioning failed, continue anyway
    }
    
    # Build output lines with padding to clear previous content
    $line1 = "Progress: $($completed.ToString('N0')) / $($iterations.ToString('N0')) rolls ($percent%)".PadRight(100)
    $line2 = "".PadRight(100)
    $line3 = "LIVE QUALITY DISTRIBUTION:".PadRight(100)
    
    $sPercent = if ($completed -gt 0) { "($([math]::Round(($sync.SRolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line4 = "  S TIER (>=6.27):       $($sync.SRolls.ToString('N0').PadLeft(10)) $sPercent".PadRight(100)
    
    $aPercent = if ($completed -gt 0) { "($([math]::Round(($sync.ARolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line5 = "  A TIER (5.92-6.27):    $($sync.ARolls.ToString('N0').PadLeft(10)) $aPercent".PadRight(100)
    
    $bPercent = if ($completed -gt 0) { "($([math]::Round(($sync.BRolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line6 = "  B TIER (5.57-5.92):    $($sync.BRolls.ToString('N0').PadLeft(10)) $bPercent".PadRight(100)
    
    $cPercent = if ($completed -gt 0) { "($([math]::Round(($sync.CRolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line7 = "  C TIER (5.23-5.57):    $($sync.CRolls.ToString('N0').PadLeft(10)) $cPercent".PadRight(100)
    
    $dPercent = if ($completed -gt 0) { "($([math]::Round(($sync.DRolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line8 = "  D TIER (4.88-5.23):    $($sync.DRolls.ToString('N0').PadLeft(10)) $dPercent".PadRight(100)
    
    $ePercent = if ($completed -gt 0) { "($([math]::Round(($sync.ERolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line9 = "  E TIER (4.44-4.88):    $($sync.ERolls.ToString('N0').PadLeft(10)) $ePercent".PadRight(100)
    
    $fPercent = if ($completed -gt 0) { "($([math]::Round(($sync.FRolls / $completed) * 100, 2))%)" } else { "(0%)" }
    $line10 = "  F TIER (<4.44):        $($sync.FRolls.ToString('N0').PadLeft(10)) $fPercent".PadRight(100)
    
    # Write all lines with colors (use carriage return to overwrite)
    Write-Host "`r$line1" -ForegroundColor Cyan -NoNewline
    Write-Host ""
    Write-Host "`r$line2" -NoNewline
    Write-Host ""
    Write-Host "`r$line3" -ForegroundColor Yellow -NoNewline
    Write-Host ""
    Write-Host "`r$line4" -ForegroundColor Green -NoNewline      # S Tier
    Write-Host ""
    Write-Host "`r$line5" -ForegroundColor Cyan -NoNewline       # A Tier
    Write-Host ""
    Write-Host "`r$line6" -ForegroundColor Blue -NoNewline       # B Tier
    Write-Host ""
    Write-Host "`r$line7" -ForegroundColor Yellow -NoNewline     # C Tier
    Write-Host ""
    Write-Host "`r$line8" -ForegroundColor Magenta -NoNewline    # D Tier
    Write-Host ""
    Write-Host "`r$line9" -ForegroundColor DarkYellow -NoNewline # E Tier
    Write-Host ""
    Write-Host "`r$line10" -ForegroundColor Red -NoNewline       # F Tier
    Write-Host ""
    
    Start-Sleep -Milliseconds 150
    
    # Check for Ctrl+C
    if ([Console]::KeyAvailable) {
        $key = [Console]::ReadKey($true)
        if ($key.Key -eq 'C' -and $key.Modifiers -eq 'Control') {
            $ctrlCPressed = $true
            Write-Host ""
            Write-Host ""
            Write-Host "Ctrl+C detected! Stopping simulation and collecting results..." -ForegroundColor Yellow
            break
        }
    }
}

# Unregister Ctrl+C handler
try {
    Unregister-Event -SourceIdentifier PowerShell.Exiting -ErrorAction SilentlyContinue
} catch {
    # Ignore errors
}

# If stopped early, wait a bit for threads to finish current work
if ($ctrlCPressed) {
    Start-Sleep -Milliseconds 500
}

# Final update to ensure 100%
try {
    [Console]::SetCursorPosition(0, $cursorTop)
} catch {
    # Cursor positioning failed, just continue
}

$line1 = "Progress: $($iterations.ToString('N0')) / $($iterations.ToString('N0')) rolls (100.0%)".PadRight(100)
$line2 = "".PadRight(100)
$line3 = "FINAL QUALITY DISTRIBUTION:".PadRight(100)
$line4 = "  S TIER (>=6.27):       $($sync.SRolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.SRolls / $iterations) * 100, 2))%)".PadRight(100)
$line5 = "  A TIER (5.92-6.27):    $($sync.ARolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.ARolls / $iterations) * 100, 2))%)".PadRight(100)
$line6 = "  B TIER (5.57-5.92):    $($sync.BRolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.BRolls / $iterations) * 100, 2))%)".PadRight(100)
$line7 = "  C TIER (5.23-5.57):    $($sync.CRolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.CRolls / $iterations) * 100, 2))%)".PadRight(100)
$line8 = "  D TIER (4.88-5.23):    $($sync.DRolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.DRolls / $iterations) * 100, 2))%)".PadRight(100)
$line9 = "  E TIER (4.44-4.88):    $($sync.ERolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.ERolls / $iterations) * 100, 2))%)".PadRight(100)
$line10 = "  F TIER (<4.44):        $($sync.FRolls.ToString('N0').PadLeft(10)) ($([math]::Round(($sync.FRolls / $iterations) * 100, 2))%)".PadRight(100)

Write-Host "`r$line1" -ForegroundColor Green
Write-Host "`r$line2"
Write-Host "`r$line3" -ForegroundColor Yellow
Write-Host "`r$line4" -ForegroundColor Green       # S Tier
Write-Host "`r$line5" -ForegroundColor Cyan        # A Tier
Write-Host "`r$line6" -ForegroundColor Blue        # B Tier
Write-Host "`r$line7" -ForegroundColor Yellow      # C Tier
Write-Host "`r$line8" -ForegroundColor Magenta     # D Tier
Write-Host "`r$line9" -ForegroundColor DarkYellow  # E Tier
Write-Host "`r$line10" -ForegroundColor Red        # F Tier

Write-Host ""

# Collect results from all threads (including incomplete ones if stopped early)
$results = @()
foreach ($job in $jobs) {
    try {
        if ($job.Handle.IsCompleted) {
            $results += $job.PowerShell.EndInvoke($job.Handle)
        } else {
            # Job not completed, try to stop it gracefully
            $job.PowerShell.Stop()
        }
        $job.PowerShell.Dispose()
    } catch {
        # Ignore errors from stopping/disposing
    }
}

# Clean up runspace pool
$runspacePool.Close()
$runspacePool.Dispose()

$stopwatch.Stop()

# Use actual completed rolls from sync hash
$actualRollsCompleted = $sync.CompletedRolls

if ($ctrlCPressed) {
    Write-Host ""
    Write-Host "Simulation STOPPED EARLY in $([math]::Round($stopwatch.Elapsed.TotalSeconds, 2)) seconds" -ForegroundColor Yellow
    Write-Host "Completed $($actualRollsCompleted.ToString('N0')) of $($iterations.ToString('N0')) rolls ($([math]::Round(($actualRollsCompleted / $iterations) * 100, 1))%)" -ForegroundColor Yellow
} else {
    Write-Host "Simulation completed in $([math]::Round($stopwatch.Elapsed.TotalSeconds, 2)) seconds" -ForegroundColor Green
}
Write-Host ""

# Aggregate results from all threads
$successCount = ($results | ForEach-Object { $_.SuccessCount } | Measure-Object -Sum).Sum

# Aggregate incremental statistics
$globalMin = ($results | ForEach-Object { $_.Min } | Measure-Object -Minimum).Minimum
$globalMax = ($results | ForEach-Object { $_.Max } | Measure-Object -Maximum).Maximum
$globalSum = ($results | ForEach-Object { $_.Sum } | Measure-Object -Sum).Sum
$globalCount = ($results | ForEach-Object { $_.Count } | Measure-Object -Sum).Sum

# Combine histograms from all threads
$combinedHistogram = New-Object int[] $histBins
foreach ($result in $results) {
    for ($i = 0; $i -lt $histBins; $i++) {
        $combinedHistogram[$i] += $result.Histogram[$i]
    }
}

# Update iterations to actual completed count for accurate statistics
$effectiveIterations = if ($globalCount -gt 0) { $globalCount } else { $actualRollsCompleted }

# Calculate percentiles from combined histogram
function Get-PercentileFromHistogram {
    param($histogram, $histMin, $histMax, $histBins, $percentile, $totalCount)
    
    if ($totalCount -eq 0) { return 0 }
    
    $targetCount = [Math]::Floor($totalCount * $percentile)
    $cumulative = 0
    $histBinWidth = ($histMax - $histMin) / $histBins
    
    for ($i = 0; $i -lt $histBins; $i++) {
        $cumulative += $histogram[$i]
        if ($cumulative -ge $targetCount) {
            # Linear interpolation within the bin
            $binStart = $histMin + ($i * $histBinWidth)
            $binEnd = $histMin + (($i + 1) * $histBinWidth)
            if ($i -gt 0) {
                $prevCumulative = $cumulative - $histogram[$i]
                $ratio = ($targetCount - $prevCumulative) / $histogram[$i]
            } else {
                $ratio = $targetCount / $histogram[$i]
            }
            return $binStart + ($binEnd - $binStart) * $ratio
        }
    }
    return $histMax
}

# Calculate statistics from histogram and aggregated data
$min = $globalMin
$max = $globalMax
$avg = if ($globalCount -gt 0) { $globalSum / $globalCount } else { 0 }
$median = Get-PercentileFromHistogram $combinedHistogram $histMin $histMax $histBins 0.50 $globalCount
$p90 = Get-PercentileFromHistogram $combinedHistogram $histMin $histMax $histBins 0.90 $globalCount
$p95 = Get-PercentileFromHistogram $combinedHistogram $histMin $histMax $histBins 0.95 $globalCount
$p99 = Get-PercentileFromHistogram $combinedHistogram $histMin $histMax $histBins 0.99 $globalCount

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Green
Write-Host "  RESULTS" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
Write-Host ""

# Calculate probability (using effective iterations from actual completed rolls)
$probability = if ($effectiveIterations -gt 0) { ($successCount / $effectiveIterations) * 100 } else { 0 }
$odds = if ($successCount -gt 0) { [math]::Round($effectiveIterations / $successCount, 1) } else { "Infinite" }

Write-Host ("Simulations Run: {0}" -f $effectiveIterations.ToString('N0')) -ForegroundColor Cyan
if ($ctrlCPressed -and $effectiveIterations -lt $iterations) {
    Write-Host ("  (Stopped early - was targeting {0})" -f $iterations.ToString('N0')) -ForegroundColor DarkGray
}
Write-Host ""
Write-Host ("Target: {0} m³/s" -f $targetSpeed) -ForegroundColor Magenta
Write-Host ("Success Count: {0}" -f $successCount.ToString('N0')) -ForegroundColor Green
Write-Host ""
Write-Host ("PROBABILITY: {0}%  |  ODDS: 1 in {1}" -f [math]::Round($probability, 2), $odds) -ForegroundColor Yellow -BackgroundColor DarkGreen
Write-Host ""

# Statistics are now calculated from histogram above (no need to sort arrays!)

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  DISTRIBUTION STATISTICS" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Minimum Speed:    $([math]::Round($min, 2)) m³/s" -ForegroundColor Red
Write-Host "Average Speed:    $([math]::Round($avg, 2)) m³/s" -ForegroundColor White
Write-Host "Median Speed:     $([math]::Round($median, 2)) m³/s" -ForegroundColor White
Write-Host "90th Percentile:  $([math]::Round($p90, 2)) m³/s" -ForegroundColor Yellow
Write-Host "95th Percentile:  $([math]::Round($p95, 2)) m³/s" -ForegroundColor Yellow
Write-Host "99th Percentile:  $([math]::Round($p99, 2)) m³/s" -ForegroundColor Green
Write-Host "Maximum Speed:    $([math]::Round($max, 2)) m³/s" -ForegroundColor Green
Write-Host ""

# Break down by quality tiers (already tracked in sync hash, no need to recalculate!)
$sRolls = $sync.SRolls
$aRolls = $sync.ARolls
$bRolls = $sync.BRolls
$cRolls = $sync.CRolls
$dRolls = $sync.DRolls
$eRolls = $sync.ERolls
$fRolls = $sync.FRolls

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  QUALITY TIER BREAKDOWN" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host ("{0,-15} {1,-25} {2}" -f "Tier", "Base (m³/s)", "Probability") -ForegroundColor Yellow
Write-Host ("-" * 80) -ForegroundColor Gray

# S TIER
$sBase = ">=6.27"
$sPercent = [math]::Round(($sRolls / $effectiveIterations) * 100, 2)
$sOdds = [math]::Round($effectiveIterations / [math]::Max($sRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "S TIER", $sBase, $sPercent, $sOdds) -ForegroundColor Green

# A TIER
$aBaseMin = 5.92
$aBaseMax = 6.27
$aBaseRange = "$aBaseMin-$aBaseMax"
$aPercent = [math]::Round(($aRolls / $effectiveIterations) * 100, 2)
$aOdds = [math]::Round($effectiveIterations / [math]::Max($aRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "A TIER", $aBaseRange, $aPercent, $aOdds) -ForegroundColor Cyan

# B TIER
$bBaseMin = 5.57
$bBaseMax = 5.92
$bBaseRange = "$bBaseMin-$bBaseMax"
$bPercent = [math]::Round(($bRolls / $effectiveIterations) * 100, 2)
$bOdds = [math]::Round($effectiveIterations / [math]::Max($bRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "B TIER", $bBaseRange, $bPercent, $bOdds) -ForegroundColor Blue

# C TIER
$cBaseMin = 5.23
$cBaseMax = 5.57
$cBaseRange = "$cBaseMin-$cBaseMax"
$cPercent = [math]::Round(($cRolls / $effectiveIterations) * 100, 2)
$cOdds = [math]::Round($effectiveIterations / [math]::Max($cRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "C TIER", $cBaseRange, $cPercent, $cOdds) -ForegroundColor Yellow

# D TIER
$dBaseMin = 4.88
$dBaseMax = 5.23
$dBaseRange = "$dBaseMin-$dBaseMax"
$dPercent = [math]::Round(($dRolls / $effectiveIterations) * 100, 2)
$dOdds = [math]::Round($effectiveIterations / [math]::Max($dRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "D TIER", $dBaseRange, $dPercent, $dOdds) -ForegroundColor Magenta

# E TIER
$eBaseMin = 4.44
$eBaseMax = 4.88
$eBaseRange = "$eBaseMin-$eBaseMax"
$ePercent = [math]::Round(($eRolls / $effectiveIterations) * 100, 2)
$eOdds = [math]::Round($effectiveIterations / [math]::Max($eRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "E TIER", $eBaseRange, $ePercent, $eOdds) -ForegroundColor DarkYellow

# F TIER
$fBase = "<4.44"
$fPercent = [math]::Round(($fRolls / $effectiveIterations) * 100, 2)
$fOdds = [math]::Round($effectiveIterations / [math]::Max($fRolls, 1), 1)
Write-Host ("{0,-15} {1,-25} {2}% (1 in {3})" -f "F TIER", $fBase, $fPercent, $fOdds) -ForegroundColor Red

Write-Host ""

# Combined good+ rolls (S + A + B tiers)
$keepableRolls = $sRolls + $aRolls + $bRolls
$keepablePercent = [math]::Round(($keepableRolls / $effectiveIterations) * 100, 2)
$keepableOdds = [math]::Round($effectiveIterations / [math]::Max($keepableRolls, 1), 1)
Write-Host ""
Write-Host ("{0,-50} {1}% (1 in {2})" -f "KEEPABLE ROLLS (5.57+ m³/s - S+A+B):", $keepablePercent, $keepableOdds) -ForegroundColor Yellow -BackgroundColor DarkGreen
Write-Host ""

# Investment analysis
Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  INVESTMENT ANALYSIS (Target: $targetTier Tier $targetSpeed m³/s)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

$targetRolls = if ($odds -eq "Infinite") { "N/A" } else { [math]::Ceiling($odds) }
Write-Host ("Expected rolls to get {0} Tier ({1} m³/s): {2}" -f $targetTier, $targetSpeed, $targetRolls) -ForegroundColor Yellow
Write-Host ""

if ($probability -gt 0) {
    Write-Host "Confidence Intervals:" -ForegroundColor Yellow
    $confidence50 = [math]::Ceiling([math]::Log(0.5) / [math]::Log(1 - ($probability / 100)))
    $confidence90 = [math]::Ceiling([math]::Log(0.1) / [math]::Log(1 - ($probability / 100)))
    $confidence99 = [math]::Ceiling([math]::Log(0.01) / [math]::Log(1 - ($probability / 100)))
    
    Write-Host "  50% chance: $confidence50 rolls"
    Write-Host "  90% chance: $confidence90 rolls"
    Write-Host "  99% chance: $confidence99 rolls"
    Write-Host ""
    
    $prob10 = (1 - [math]::Pow((1 - ($probability / 100)), 10)) * 100
    Write-Host ("10 rolls: {0}% chance of getting {1} Tier" -f [math]::Round($prob10, 2), $targetTier) -ForegroundColor Cyan
    
    $prob50 = (1 - [math]::Pow((1 - ($probability / 100)), 50)) * 100
    Write-Host ("50 rolls: {0}% chance of getting {1} Tier" -f [math]::Round($prob50, 2), $targetTier) -ForegroundColor Cyan
    Write-Host ""
}

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "Script completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Press ENTER to exit..." -ForegroundColor Gray
$null = Read-Host
