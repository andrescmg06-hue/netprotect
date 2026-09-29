package com.netprotect.app.feature.home

/** What the sign-in screen shows about the backend. It is the only thing the screen learns about
 * infrastructure: no database/Redis detail is shown to the user (same decision as the web login,
 * Sprint 39 D2). `Ready` means /health/ready answered "ready" for backend, database and Redis. */
enum class ServiceStatus { Checking, Ready, Unavailable }
