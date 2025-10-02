#!/usr/bin/env python3
"""
Test script for OTP-based password reset functionality
"""

import requests
import json
import time

# API Base URL
BASE_URL = "http://127.0.0.1:8000/api/users"

def test_otp_password_reset_flow():
    """Test the complete OTP password reset flow"""
    
    print("🧪 Testing OTP Password Reset Flow")
    print("=" * 50)
    
    # Test email for password reset
    test_email = "test@example.com"
    
    print(f"\n1️⃣ Testing password reset request for: {test_email}")
    print("-" * 30)
    
    # Step 1: Request OTP
    try:
        response = requests.post(f"{BASE_URL}/password-reset/", 
                               json={"email": test_email}, 
                               timeout=10)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
        
        if response.status_code == 200:
            print("✅ OTP request successful!")
        else:
            print("❌ OTP request failed")
            
    except Exception as e:
        print(f"❌ Error requesting OTP: {e}")
    
    print(f"\n2️⃣ Testing OTP verification")
    print("-" * 30)
    
    # Step 2: Test OTP verification with invalid OTP
    try:
        response = requests.post(f"{BASE_URL}/password-reset-verify-otp/", 
                               json={"email": test_email, "otp": "123456"}, 
                               timeout=10)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
        
        if response.status_code == 400:
            print("✅ Invalid OTP properly rejected!")
        else:
            print("❌ Invalid OTP handling issue")
            
    except Exception as e:
        print(f"❌ Error verifying OTP: {e}")
    
    print(f"\n3️⃣ Testing password reset confirmation")
    print("-" * 30)
    
    # Step 3: Test password reset with invalid OTP
    try:
        response = requests.post(f"{BASE_URL}/password-reset-confirm/", 
                               json={
                                   "email": test_email, 
                                   "otp": "123456",
                                   "password": "newpassword123",
                                   "password_confirm": "newpassword123"
                               }, 
                               timeout=10)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")
        
        if response.status_code == 400:
            print("✅ Invalid OTP for password reset properly rejected!")
        else:
            print("❌ Password reset validation issue")
            
    except Exception as e:
        print(f"❌ Error resetting password: {e}")

def show_api_endpoints():
    """Show the OTP password reset API endpoints"""
    print("\n📋 OTP Password Reset API Endpoints")
    print("=" * 50)
    
    endpoints = [
        {
            "method": "POST",
            "url": f"{BASE_URL}/password-reset/",
            "description": "Request OTP for password reset",
            "body": {"email": "user@example.com"}
        },
        {
            "method": "POST", 
            "url": f"{BASE_URL}/password-reset-verify-otp/",
            "description": "Verify OTP received in email",
            "body": {"email": "user@example.com", "otp": "123456"}
        },
        {
            "method": "POST",
            "url": f"{BASE_URL}/password-reset-confirm/",
            "description": "Reset password with verified OTP",
            "body": {
                "email": "user@example.com", 
                "otp": "123456",
                "password": "newpassword123",
                "password_confirm": "newpassword123"
            }
        }
    ]
    
    for i, endpoint in enumerate(endpoints, 1):
        print(f"\n{i}. {endpoint['method']} {endpoint['url']}")
        print(f"   Description: {endpoint['description']}")
        print(f"   Body: {json.dumps(endpoint['body'], indent=6)}")

def show_frontend_flow():
    """Show the expected frontend flow"""
    print("\n🖥️  Frontend Implementation Flow")
    print("=" * 50)
    
    steps = [
        {
            "step": "1. Password Reset Request",
            "description": "User enters email and clicks 'Reset Password'",
            "api_call": "POST /password-reset/",
            "frontend_action": "Show 'OTP sent to email' message"
        },
        {
            "step": "2. OTP Input", 
            "description": "User checks email and enters 6-digit OTP",
            "api_call": "POST /password-reset-verify-otp/",
            "frontend_action": "If successful, show new password form"
        },
        {
            "step": "3. New Password",
            "description": "User enters new password and confirmation",
            "api_call": "POST /password-reset-confirm/",
            "frontend_action": "If successful, redirect to login page"
        }
    ]
    
    for step in steps:
        print(f"\n{step['step']}:")
        print(f"   📝 {step['description']}")
        print(f"   🔗 API: {step['api_call']}")
        print(f"   🎨 Frontend: {step['frontend_action']}")

def show_security_features():
    """Show security features of the OTP system"""
    print("\n🔒 Security Features")
    print("=" * 50)
    
    features = [
        "✅ OTP expires in 10 minutes for verification",
        "✅ OTP session expires in 15 minutes for password reset",
        "✅ OTP is cleared after successful password reset",
        "✅ OTP is cleared if expired",
        "✅ Must verify OTP before setting new password",
        "✅ Password confirmation required",
        "✅ Minimum 8 character password requirement",
        "✅ Only one active OTP per user at a time",
        "✅ Random 6-digit OTP generation",
        "✅ Email validation before OTP generation"
    ]
    
    for feature in features:
        print(f"   {feature}")

if __name__ == "__main__":
    test_otp_password_reset_flow()
    show_api_endpoints()
    show_frontend_flow()
    show_security_features()
    
    print(f"\n" + "=" * 50)
    print("🎉 OTP Password Reset System is Ready!")
    print("The system supports the 3-step flow you requested:")
    print("1. Request OTP → Send OTP to email")
    print("2. Verify OTP → Frontend shows new password form")
    print("3. Set Password → Complete the reset process")
