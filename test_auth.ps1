$base = "http://localhost:8080/api/auth"
$email = "test" + (Get-Random) + "@example.com"
$password = "password123"

Write-Host "Registering $email..."
$signupBody = @{
    fullName = "Test User"
    email = $email
    password = $password
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "$base/signup" -Method Post -Body $signupBody -ContentType "application/json"
    Write-Host "Signup Success: $($res.message)"
} catch {
    Write-Host "Signup Failed: $_"
    exit
}

Write-Host "Logging in..."
$loginBody = @{
    email = $email
    password = $password
} | ConvertTo-Json

try {
    $res = Invoke-RestMethod -Uri "$base/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login Success: $($res.message)"
    Write-Host "API Key: $($res.apiKey)"
} catch {
    $e = $_.Exception
    $resp = $e.Response
    if ($resp) {
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        Write-Host "Login Failed: $($e.Message)"
        Write-Host "Server Response: $errBody"
    } else {
        Write-Host "Login Failed: $($e.Message)"
    }
}
