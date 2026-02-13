# Mobile Infra for Lambdas and Files (MILF)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Go](https://img.shields.io/badge/Server-Go-blue?logo=go)](https://github.com/adarshxsh/milf/tree/development/CentralServer)
[![Flutter](https://img.shields.io/badge/Client-Flutter-cyan?logo=flutter)](https://github.com/adarshxsh/milf/tree/Consumer/consumer)

**Mobile Infra for Lambdas and Files (MILF)** is a distributed edge-computing framework. It enables the execution of serverless functions (**Lambdas**) and the management of distributed data (**Files**) across mobile devices. By utilizing WebAssembly (WASM), it provides a secure, sandboxed environment that turns standard smartphones into powerful compute nodes.

---

## 🏗️ Detailed Project Architecture

The system is built on a distributed architecture where tasks flow from a **Client** through a **Central Server** to **Consumer Nodes**, with final data persistence at a **Sink**.

### 1. Central Server (Go-based Orchestration)
The brain of the system, written in Go, handles the heavy lifting of coordination.
- **Reference**: [`/CentralServer`](https://github.com/adarshxsh/milf/tree/development/CentralServer)
- **Functions**: Auth, Compiler service (WASM generation), Orchestration, and Queue management.

### 2. Consumer Node (Flutter & Native C++ Execution)
The execution engine that runs on Android/iOS.
- **Reference**: [`/consumer`](https://github.com/adarshxsh/milf/tree/Consumer/consumer)
- **Functions**: Securely hosts the **Wasm Micro Runtime (WAMR)** and **Wasm3** inside an isolated sandbox.

### 3. Client Dashboard (React/Vite Frontend)
The portal where users submit and monitor their "Lambdas".
- **Reference**: [`/Client`](https://github.com/adarshxsh/milf/tree/Client/Client)
- **Functions**: Job submission, real-time metrics visualization, and user management.

### 4. Sink (Termination & Persistence)
The final destination for data processed by the lambdas.
- **Reference**: [`/sink`](https://github.com/adarshxsh/milf/tree/Sink/sink)
- **Functions**: Data aggregation, log persistence, and result verified storage.

---

## 🌿 Branch-wise Codebase Analysis

The repository is modularly structured across multiple branches. Each branch focuses on a specific pillar of the **Mobile Infra for Lambdas and Files** ecosystem.

### [Main Branch](https://github.com/adarshxsh/milf/tree/main)
*Focus: Root Infrastructure and Native Sandboxing*
- **[`/wamr_sandbox`](https://github.com/adarshxsh/milf/tree/main/wamr_sandbox)**: Contains the core C/C++ implementation of the WASM sandbox environment. 
- **[`/sinkend`](https://github.com/adarshxsh/milf/tree/main/sinkend)**: The terminal logic for data sinking on the main branch.
- **[`plan.json`](https://github.com/adarshxsh/milf/blob/main/plan.json)**: The master architectural blueprint of the entire system.

### [Development Branch](https://github.com/adarshxsh/milf/tree/development)
*Focus: Backend Orchestration & Go Microservices*
- **Key Files/Folders**:
    - [`CentralServer/cmd/server/main.go`](https://github.com/adarshxsh/milf/blob/development/CentralServer/cmd/server/main.go): Server entry point and dependency injection setup.
    - [`CentralServer/internal/orchestrator`](https://github.com/adarshxsh/milf/tree/development/CentralServer/internal/orchestrator): Core logic for task lifecycle management.
    - [`CentralServer/internal/compiler`](https://github.com/adarshxsh/milf/tree/development/CentralServer/internal/compiler): On-the-fly compilation of source code into WASM binaries.
    - [`CentralServer/internal/storage/memory_store.go`](https://github.com/adarshxsh/milf/blob/development/CentralServer/internal/storage/memory_store.go): In-memory persistence for rapid job state transitions.

### [Auth Branch](https://github.com/adarshxsh/milf/tree/Auth)
*Focus: Security & Identity Management*
- **[`CentralServer/internal/auth`](https://github.com/adarshxsh/milf/tree/Auth/CentralServer/internal/auth)**: Implements authentication handlers, JWT-based security policies, and user permission models.

### [Client Branch](https://github.com/adarshxsh/milf/tree/Client)
*Focus: Distributed Task Management Frontend*
- **[`/Client`](https://github.com/adarshxsh/milf/tree/Client/Client)**: A React/Vite application for end-user interaction.
    - `index.html`: Main UI wrapper.
    - `components.json`: Reusable UI element configurations.

### [Consumer Branch](https://github.com/adarshxsh/milf/tree/Consumer)
*Focus: WebAssembly Execution Node*
- **[`/consumer`](https://github.com/adarshxsh/milf/tree/Consumer/consumer)**: The production Flutter application.
    - [`lib/modules/native_bridge/`](https://github.com/adarshxsh/milf/tree/Consumer/consumer/lib/modules/native_bridge): The bridge between Flutter and the C++ WASM runtime.
    - [`lib/modules/cloud_connect/`](https://github.com/adarshxsh/milf/tree/Consumer/consumer/lib/modules/cloud_connect): Manages WebSocket/REST links to the Central Server.
    - [`lib/main.dart`](https://github.com/adarshxsh/milf/blob/Consumer/consumer/lib/main.dart): Orchestrates the mobile node startup.

### [Sink Branch](https://github.com/adarshxsh/milf/tree/Sink)
*Focus: Data Termination & Optimized Execution*
- **[`/consumeronlywamr`](https://github.com/adarshxsh/milf/tree/Sink/consumeronlywamr)**: A lightweight, WAMR-only build of the consumer node, optimized for low-resource environments.
- **[`/sink`](https://github.com/adarshxsh/milf/tree/Sink/sink)**: The specialized Flutter implementation for data collection and visualization.

---

## 🛠️ Key Components (Full Form)

| Component | Repository Folder | Branch | Description |
| :--- | :--- | :--- | :--- |
| **Orchestration System** | [`/CentralServer`](https://github.com/adarshxsh/milf/tree/development/CentralServer) | `development` | Go-based core for job scheduling |
| **Identity Service** | [`/Auth`](https://github.com/adarshxsh/milf/tree/Auth/CentralServer/internal/auth) | `Auth` | User authentication and access control |
| **Task Management Frontend**| [`/Client`](https://github.com/adarshxsh/milf/tree/Client/Client) | `Client` | React Dashboard for monitoring tasks |
| **Mobile Execution Node** | [`/consumer`](https://github.com/adarshxsh/milf/tree/Consumer/consumer) | `Consumer` | Flutter app running the WASM sandbox |
| **Lightweight Runner** | [`/consumeronlywamr`](https://github.com/adarshxsh/milf/tree/Sink/consumeronlywamr) | `Sink` | High-performance WAMR-focused node |
| **Data Termination Point** | [`/sink`](https://github.com/adarshxsh/milf/tree/Sink/sink) | `Sink` | Final destination for lambda outputs |

---

## 🛡️ Security & Isolation
- **Linux Namespaces**: PID, Mount, and Network namespaces isolate the WASM process from the host.
- **Seccomp Filters**: Restricts host system calls to a pre-defined whitelist.
- **Resource Quotas**: Hard-coded limits on memory (RSS) and execution time to prevent resource exhaustion.

---

## 📄 License
This fork is licensed under the Apache License 2.0. See the LICENSE file for details.

Developed with ❤️ by [Devlup Labs](https://github.com/devlup-labs)
