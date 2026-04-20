# GraphQL Schema Design — E-Commerce Platform

Design a complete schema with the following requirements:
1. Types: Product, Order, User, Review, Cart
2. Queries with search, filter, cursor-based pagination
3. Mutations: addToCart, placeOrder, writeReview
4. Subscriptions: orderStatusChanged
5. Input types and enums

```graphql
# YOUR SCHEMA HERE
```

## Design Decisions

Document your reasoning for:
- Why cursor-based vs offset pagination?
- How do you handle authorization at field level?
- How do you prevent N+1 queries?
- How do you handle errors (union types vs error extensions)?
