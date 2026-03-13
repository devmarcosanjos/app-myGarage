# 🚗 My Garagem

Aplicativo Android para gerenciamento de veículos pessoais. Permite cadastrar, editar, visualizar e excluir carros da sua garagem, com suporte a fotos, localização no mapa e sistema de favoritos.

---

## 📱 Linguagem e Tecnologias

| Tecnologia | Uso |
|---|---|
| **Kotlin** | Linguagem principal do projeto |
| **Android SDK** (minSdk 24 / targetSdk 36) | Plataforma mobile |
| **Material Design 3** | Componentes visuais e tema |
| **Firebase Auth** | Autenticação via SMS (telefone) |
| **Firebase Storage** | Armazenamento de fotos dos veículos |
| **Retrofit + Gson** | Comunicação com API REST |
| **Room** | Banco de dados local (SQLite) |
| **Google Maps SDK** | Exibição de mapas e seleção de localização |
| **Picasso** | Carregamento e cache de imagens |
| **Coroutines** | Operações assíncronas |
| **ViewBinding / DataBinding** | Vinculação de views |
| **KSP** | Processamento de anotações (Room) |

---

## ⚙️ Funcionalidades

### Autenticação
- Login via **SMS (OTP)** com Firebase Phone Authentication
- Verificação de código SMS com timeout de 45 segundos
- Redirecionamento automático para tela principal se já autenticado
- Logout com limpeza de sessão Firebase

### Gerenciamento de Carros (CRUD)
- **Listar** todos os carros em cards com imagem, nome, ano e placa
- **Adicionar** novo carro com nome, ano, placa, foto e localização
- **Editar** informações de um carro existente
- **Excluir** carro com diálogo de confirmação (remove também a imagem do Firebase Storage)

### Imagens
- Captura de foto pela **câmera** do dispositivo
- Inserção de imagem via **URL** manual
- Upload automático para **Firebase Storage** (compressão JPEG 80%)
- Placeholder durante carregamento e ícone de erro em caso de falha

### Localização e Mapa
- Captura da **localização atual** do usuário (GPS)
- Seleção de localização no **mapa interativo** (toque para posicionar marcador)
- Exibição do mapa com marcador na tela de detalhes do carro
- Envio de geolocalização como header HTTP em todas as requisições da API

### Favoritos
- Botão de **coração** nos cards para favoritar/desfavoritar
- Persistência local via **SharedPreferences**
- Filtro para exibir apenas carros favoritados

### Filtros e Ordenação
- **Recentes** — ordem original da API
- **A-Z** — ordenação alfabética crescente
- **Z-A** — ordenação alfabética decrescente
- **Favoritos** — apenas carros marcados como favoritos

### Outras Funcionalidades
- **Swipe-to-refresh** para atualizar a lista de carros
- **FAB** (Floating Action Button) para adicionar novo carro
- Toolbar com menu de logout

---

## 📐 Regras de Negócio

1. **Autenticação obrigatória** — O usuário deve estar autenticado via SMS para acessar o app. Sem login, não é possível visualizar ou gerenciar carros.

2. **Campos obrigatórios para cadastro** — Nome, ano, placa e localização são obrigatórios para criar um novo carro. A imagem é opcional.

3. **Imagem do carro** — O usuário pode escolher entre tirar uma foto com a câmera ou informar uma URL. Fotos capturadas são enviadas ao Firebase Storage com compressão JPEG (80% de qualidade).

4. **Exclusão com confirmação** — A exclusão de um carro exige confirmação do usuário via diálogo. Ao excluir, a imagem associada no Firebase Storage também é removida.

5. **Favoritos são locais** — O sistema de favoritos é persistido apenas no dispositivo via SharedPreferences. Não há sincronização com o backend.

6. **Geolocalização nas requisições** — Toda requisição à API inclui os headers `x-data-latitude` e `x-data-longitude` com a última localização conhecida do usuário, obtida via Room.

7. **Permissões em tempo de execução** — O app solicita permissão de localização (GPS) e câmera em tempo de execução, conforme as diretrizes do Android.

8. **Sessão persistente** — Se o usuário já estiver autenticado no Firebase, ele é redirecionado automaticamente para a tela principal sem precisar fazer login novamente.

9. **ID único por carro** — Cada carro recebe um UUID gerado pelo app no momento do cadastro.

10. **Atualização da lista** — Ao voltar da tela de detalhes, edição ou adição, a lista de carros é atualizada automaticamente via `ActivityResultLauncher`.

---

## 🏗️ Arquitetura do Projeto

```
app/src/main/java/com/marcosanjos/mygaragem/
├── LoginActivity.kt          # Tela de login (SMS)
├── MainActivity.kt           # Tela principal (lista de carros)
├── CarDetailsActivity.kt     # Detalhes do carro + mapa
├── AddCarActivity.kt         # Cadastro de novo carro
├── EditCarActivity.kt        # Edição de carro
├── FavoritesManager.kt       # Gerenciador de favoritos (SharedPreferences)
│
├── adapter/
│   └── CarAdapter.kt         # Adapter do RecyclerView
│
├── model/
│   └── Car.kt                # Modelos: Car, CarLocation, CarDetailResponse
│
├── service/
│   ├── CarApiService.kt      # Interface Retrofit (endpoints)
│   ├── RetrofitClient.kt     # Configuração do cliente HTTP
│   ├── GeoLocationInterceptor.kt  # Interceptor de geolocalização
│   └── SafeApiCall.kt        # Wrapper de resultado (Success/Error)
│
├── database/
│   ├── AppDatabase.kt        # Banco Room
│   ├── UserLocation.kt       # Entidade de localização
│   ├── UserLocationDao.kt    # DAO de localização
│   ├── DatabaseBuilder.kt    # Singleton do banco
│   └── DateConverters.kt     # Conversor Date ↔ Long
│
└── ui/
    ├── CircleTransform.kt    # Transformação circular (Picasso)
    └── ImageUrl.kt           # BindingAdapter para imagens
```

---

## 🔑 Configuração

### Pré-requisitos
- Android Studio
- JDK 11+
- Conta Firebase com projeto configurado

### Arquivos sensíveis (não versionados)
| Arquivo | Descrição |
|---|---|
| `app/google-services.json` | Configuração do Firebase (obtido no console Firebase) |
| `local.properties` | Contém `MAPS_API_KEY` para o Google Maps |

### Passos
1. Clone o repositório
2. Coloque seu `google-services.json` em `app/`
3. Adicione sua chave do Google Maps em `local.properties`:
   ```
   MAPS_API_KEY=sua_chave_aqui
   ```
4. Inicie a API backend em `localhost:3000` (o emulador acessa via `10.0.2.2:3000`)
5. Compile e execute no Android Studio

---

## 📄 Licença

Este projeto é de uso acadêmico/pessoal.