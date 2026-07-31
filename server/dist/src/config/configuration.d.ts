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
    smtp: {
        host: string;
        port: number;
        user: string;
        pass: string;
    };
};
export default _default;
