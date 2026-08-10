param(
    [int]$Port = 9999
)

$ErrorActionPreference = "Stop"

$signingSecret = $env:VEROX_WEBHOOK_SIGNING_SECRET
if ([string]::IsNullOrWhiteSpace($signingSecret) -or -not $signingSecret.StartsWith("whsec_")) {
    throw "Set VEROX_WEBHOOK_SIGNING_SECRET to the merchant whsec_* before starting the receiver."
}

function Get-HexHmacSha256 {
    param(
        [string]$Secret,
        [string]$Value
    )

    $keyBytes = [System.Text.Encoding]::UTF8.GetBytes($Secret)
    $valueBytes = [System.Text.Encoding]::UTF8.GetBytes($Value)
    $hmac = New-Object System.Security.Cryptography.HMACSHA256(,$keyBytes)
    try {
        $hash = $hmac.ComputeHash($valueBytes)
        return ([System.BitConverter]::ToString($hash)).Replace("-", "").ToLowerInvariant()
    }
    finally {
        $hmac.Dispose()
    }
}

function Convert-HexToBytes {
    param([string]$Hex)

    if ([string]::IsNullOrWhiteSpace($Hex) -or ($Hex.Length % 2) -ne 0) {
        return $null
    }

    try {
        [byte[]]$bytes = New-Object byte[] ($Hex.Length / 2)
        for ($i = 0; $i -lt $bytes.Length; $i++) {
            $bytes[$i] = [Convert]::ToByte($Hex.Substring($i * 2, 2), 16)
        }
        return $bytes
    }
    catch {
        return $null
    }
}

function Test-FixedTimeHexEqual {
    param(
        [string]$ExpectedHex,
        [string]$ActualHex
    )

    [byte[]]$expected = Convert-HexToBytes $ExpectedHex
    [byte[]]$actual = Convert-HexToBytes $ActualHex
    if ($null -eq $expected -or $null -eq $actual -or $expected.Length -ne $actual.Length) {
        return $false
    }

    $difference = 0
    for ($i = 0; $i -lt $expected.Length; $i++) {
        $difference = $difference -bor ($expected[$i] -bxor $actual[$i])
    }
    return $difference -eq 0
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
$listener.Start()

Write-Host "VEROX merchant webhook receiver listening on http://localhost:$Port/"
Write-Host "Signature validation is enabled. Press Ctrl+C to stop."

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $reader = New-Object System.IO.StreamReader($request.InputStream, $request.ContentEncoding)
        try {
            $body = $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }

        $signatureHeader = $request.Headers["VEROX-Signature"]
        $eventId = $request.Headers["VEROX-Event-Id"]
        $eventType = $request.Headers["VEROX-Event-Type"]
        $deliveryId = $request.Headers["VEROX-Delivery-Id"]

        $valid = $false
        if (-not [string]::IsNullOrWhiteSpace($signatureHeader)) {
            $timestampPart = $null
            $signaturePart = $null
            foreach ($part in $signatureHeader.Split(',')) {
                $pair = $part -split '=', 2
                if ($pair.Length -ne 2) { continue }
                if ($pair[0] -eq 't') { $timestampPart = $pair[1] }
                if ($pair[0] -eq 'v1') { $signaturePart = $pair[1] }
            }

            if ($timestampPart -match '^\d+$' -and $signaturePart -match '^[0-9a-fA-F]{64}$') {
                $expectedSignature = Get-HexHmacSha256 -Secret $signingSecret -Value "$timestampPart.$body"
                $valid = Test-FixedTimeHexEqual -ExpectedHex $expectedSignature -ActualHex $signaturePart
            }
        }

        if ($request.HttpMethod -ne 'POST' -or -not $valid) {
            $response.StatusCode = 401
            $responseBody = '{"received":false,"signature_valid":false}'
            Write-Warning "Rejected webhook delivery=$deliveryId event=$eventId type=$eventType"
        }
        else {
            $response.StatusCode = 200
            $responseBody = '{"received":true,"signature_valid":true}'
            Write-Host "Accepted webhook delivery=$deliveryId event=$eventId type=$eventType"
            Write-Host $body
        }

        $response.ContentType = 'application/json'
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($responseBody)
        $response.ContentLength64 = $bytes.Length
        $response.OutputStream.Write($bytes, 0, $bytes.Length)
        $response.OutputStream.Close()
    }
}
finally {
    $listener.Stop()
    $listener.Close()
}
