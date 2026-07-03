declare const _default: () => {
    port: number;
    jwt: {
        accessSecret: string;
        accessExpiresIn: string;
        refreshSecret: string;
        refreshExpiresIn: string;
    };
    redis: {
        host: string;
        port: number;
    };
    admin: {
        passwordHash: string;
    };
};
export default _default;
